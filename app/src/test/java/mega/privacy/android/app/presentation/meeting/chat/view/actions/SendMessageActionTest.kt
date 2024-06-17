package mega.privacy.android.app.presentation.meeting.chat.view.actions

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.domain.entity.chat.messages.ContactAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.normal.NormalMessage
import mega.privacy.android.domain.entity.chat.messages.normal.TextMessage
import mega.privacy.mobile.analytics.event.ChatConversationSendMessageActionMenuEvent
import mega.privacy.mobile.analytics.event.ChatConversationSendMessageActionMenuItemEvent
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import mega.privacy.android.core.test.AnalyticsTestRule

@RunWith(AndroidJUnit4::class)
class SendMessageActionTest {

    val composeTestRule = createAndroidComposeRule<ComponentActivity>()
    private val analyticsRule = AnalyticsTestRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(analyticsRule).around(composeTestRule)

    private lateinit var underTest: SendMessageAction

    private val chatViewModel = mock<ChatViewModel>()

    @Before
    fun setUp() {
        underTest = SendMessageAction(
            chatViewModel = chatViewModel,
        )
    }

    @Test
    fun `test that action applies to contact attachments which are my contacts`() {
        assertThat(underTest.appliesTo(setOf(mock<ContactAttachmentMessage> {
            on { isContact } doReturn true
        }))).isTrue()
    }

    @Test
    fun `test that action does not apply to contact attachments which are not my contacts`() {
        assertThat(underTest.appliesTo(setOf(mock<ContactAttachmentMessage> {
            on { isContact } doReturn false
        }))).isFalse()
    }

    @Test
    fun `test that action does not apply to non contact messages`() {
        assertThat(underTest.appliesTo(setOf(mock<NormalMessage>()))).isFalse()
    }


    @Test
    fun `test that composable contains delete bottom action`() {
        composeTestRule.setContent(
            underTest.bottomSheetMenuItem(
                messages = setOf(mock<TextMessage>()),
                hideBottomSheet = {},
                setAction = {},
            )
        )

        composeTestRule.onNodeWithTag(underTest.bottomSheetItemTestTag).assertExists()
    }

    @Test
    fun `test that analytics tracker sends the right event when message action is triggered from a bottom sheet`() {
        underTest.trackTriggerEvent(source = MessageAction.TriggerSource.BottomSheet)

        assertThat(analyticsRule.events).contains(ChatConversationSendMessageActionMenuItemEvent)
    }

    @Test
    fun `test that analytics tracker sends the right event when message action is triggered from a toolbar`() {
        underTest.trackTriggerEvent(source = MessageAction.TriggerSource.Toolbar)

        assertThat(analyticsRule.events).contains(ChatConversationSendMessageActionMenuEvent)
    }
}