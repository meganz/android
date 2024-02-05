package test.mega.privacy.android.app.presentation.meeting.chat.view.message.management

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.view.message.management.ChatCallMessageView
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.core.ui.controls.chat.messages.TEST_TAG_MANAGEMENT_MESSAGE_ICON
import mega.privacy.android.domain.entity.chat.ChatMessageTermCode
import mega.privacy.android.domain.entity.chat.messages.management.CallEndedMessage
import mega.privacy.android.domain.entity.chat.messages.management.CallMessage
import mega.privacy.android.domain.entity.chat.messages.management.CallStartedMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class ChatCallMessageViewTest {
    @get:Rule
    var composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val msgId = 123L

    @Test
    fun `test that text and icon show correctly when call started`() {
        initComposeRuleContent(
            message = CallStartedMessage(
                msgId = msgId,
                time = System.currentTimeMillis(),
                isMine = true,
                userHandle = 1234567890L,
                shouldShowAvatar = false,
                shouldShowTime = false,
                shouldShowDate = false,
                reactions = emptyList(),
            ),
            isOneToOneChat = true
        )
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(R.string.call_started_messages))
            .assertExists()
        composeTestRule.onNodeWithTag(TEST_TAG_MANAGEMENT_MESSAGE_ICON).assertExists()
    }

    @Test
    fun `test that text and icon show correctly when call end with ended term code and chat is one to one`() {
        initComposeRuleContent(
            message = CallEndedMessage(
                msgId = msgId,
                time = System.currentTimeMillis(),
                isMine = true,
                termCode = ChatMessageTermCode.ENDED,
                duration = 0.seconds,
                userHandle = 1234567890L,
                shouldShowAvatar = false,
                shouldShowTime = false,
                shouldShowDate = false,
                reactions = emptyList(),
            ),
            isOneToOneChat = true
        )
        composeTestRule.onNodeWithText("Call ended. Duration: 0 seconds").assertExists()
            .assertExists()
        composeTestRule.onNodeWithTag(TEST_TAG_MANAGEMENT_MESSAGE_ICON).assertExists()
    }

    @Test
    fun `test that text and icon show correctly when call end with ended term code and chat is group`() {
        initComposeRuleContent(
            message = CallEndedMessage(
                msgId = msgId,
                time = System.currentTimeMillis(),
                isMine = true,
                termCode = ChatMessageTermCode.ENDED,
                duration = 0.seconds,
                userHandle = 1234567890L,
                shouldShowAvatar = false,
                shouldShowTime = false,
                shouldShowDate = false,
                reactions = emptyList(),
            ),
            isOneToOneChat = false
        )
        composeTestRule.onNodeWithText(
            TextUtil.removeFormatPlaceholder(
                composeTestRule.activity.getString(R.string.group_call_ended_no_duration_message)
            )
        ).assertExists()
            .assertExists()
        composeTestRule.onNodeWithTag(TEST_TAG_MANAGEMENT_MESSAGE_ICON).assertExists()
    }

    @Test
    fun `test that text and icon show correctly when call end with rejected term code`() {
        initComposeRuleContent(
            message = CallEndedMessage(
                msgId = msgId,
                time = System.currentTimeMillis(),
                isMine = true,
                termCode = ChatMessageTermCode.REJECTED,
                duration = 0.seconds,
                userHandle = 1234567890L,
                shouldShowAvatar = false,
                shouldShowTime = false,
                shouldShowDate = false,
                reactions = emptyList(),
            ),
            isOneToOneChat = true
        )
        composeTestRule.onNodeWithText(
            TextUtil.removeFormatPlaceholder(
                composeTestRule.activity.getString(R.string.call_rejected_messages)
            )
        ).assertExists()
            .assertExists()
        composeTestRule.onNodeWithTag(TEST_TAG_MANAGEMENT_MESSAGE_ICON).assertExists()
    }

    @Test
    fun `test that text and icon show correctly when call end with no answer term code and it is my message`() {
        initComposeRuleContent(
            message = CallEndedMessage(
                msgId = msgId,
                time = System.currentTimeMillis(),
                isMine = true,
                termCode = ChatMessageTermCode.NO_ANSWER,
                duration = 0.seconds,
                userHandle = 1234567890L,
                shouldShowAvatar = false,
                shouldShowTime = false,
                shouldShowDate = false,
                reactions = emptyList(),
            ),
            isOneToOneChat = true
        )
        composeTestRule.onNodeWithText(
            TextUtil.removeFormatPlaceholder(
                composeTestRule.activity.getString(R.string.call_not_answered_messages)
            )
        ).assertExists()
            .assertExists()
        composeTestRule.onNodeWithTag(TEST_TAG_MANAGEMENT_MESSAGE_ICON).assertExists()
    }

    @Test
    fun `test that text and icon show correctly when call end with no answer term code and it is not my message`() {
        initComposeRuleContent(
            message = CallEndedMessage(
                msgId = msgId,
                time = System.currentTimeMillis(),
                isMine = false,
                termCode = ChatMessageTermCode.NO_ANSWER,
                duration = 0.seconds,
                userHandle = 1234567890L,
                shouldShowAvatar = false,
                shouldShowTime = false,
                shouldShowDate = false,
                reactions = emptyList(),
            ),
            isOneToOneChat = true
        )
        composeTestRule.onNodeWithText(
            TextUtil.removeFormatPlaceholder(
                composeTestRule.activity.getString(R.string.call_missed_messages)
            )
        ).assertExists()
            .assertExists()
        composeTestRule.onNodeWithTag(TEST_TAG_MANAGEMENT_MESSAGE_ICON).assertExists()
    }

    @Test
    fun `test that text and icon show correctly when call end with cancelled term code and it is my message`() {
        initComposeRuleContent(
            message = CallEndedMessage(
                msgId = msgId,
                time = System.currentTimeMillis(),
                isMine = true,
                termCode = ChatMessageTermCode.CANCELLED,
                duration = 0.seconds,
                userHandle = 1234567890L,
                shouldShowAvatar = false,
                shouldShowTime = false,
                shouldShowDate = false,
                reactions = emptyList(),
            ),
            isOneToOneChat = true
        )
        composeTestRule.onNodeWithText(
            TextUtil.removeFormatPlaceholder(
                composeTestRule.activity.getString(R.string.call_cancelled_messages)
            )
        ).assertExists()
            .assertExists()
        composeTestRule.onNodeWithTag(TEST_TAG_MANAGEMENT_MESSAGE_ICON).assertExists()
    }

    @Test
    fun `test that text and icon show correctly when call end with failed term code`() {
        initComposeRuleContent(
            message = CallEndedMessage(
                msgId = msgId,
                time = System.currentTimeMillis(),
                isMine = false,
                termCode = ChatMessageTermCode.CANCELLED,
                duration = 0.seconds,
                userHandle = 1234567890L,
                shouldShowAvatar = false,
                shouldShowTime = false,
                shouldShowDate = false,
                reactions = emptyList(),
            ),
            isOneToOneChat = true
        )
        composeTestRule.onNodeWithText(
            TextUtil.removeFormatPlaceholder(
                composeTestRule.activity.getString(R.string.call_missed_messages)
            )
        ).assertExists()
            .assertExists()
        composeTestRule.onNodeWithTag(TEST_TAG_MANAGEMENT_MESSAGE_ICON).assertExists()
    }

    @Test
    fun `test that text and icon show correctly when call end with cancelled term code and it is not my message`() {
        initComposeRuleContent(
            message = CallEndedMessage(
                msgId = msgId,
                time = System.currentTimeMillis(),
                isMine = false,
                termCode = ChatMessageTermCode.FAILED,
                duration = 0.seconds,
                userHandle = 1234567890L,
                shouldShowAvatar = false,
                shouldShowTime = false,
                shouldShowDate = false,
                reactions = emptyList(),
            ),
            isOneToOneChat = true
        )
        composeTestRule.onNodeWithText(
            TextUtil.removeFormatPlaceholder(
                composeTestRule.activity.getString(R.string.call_failed_messages)
            )
        ).assertExists()
            .assertExists()
        composeTestRule.onNodeWithTag(TEST_TAG_MANAGEMENT_MESSAGE_ICON).assertExists()
    }

    private fun initComposeRuleContent(
        message: CallMessage,
        isOneToOneChat: Boolean,
    ) {
        composeTestRule.setContent {
            ChatCallMessageView(message = message, isOneToOneChat = isOneToOneChat)
        }
    }
}