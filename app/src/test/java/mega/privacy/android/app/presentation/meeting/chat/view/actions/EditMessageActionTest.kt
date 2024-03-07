package mega.privacy.android.app.presentation.meeting.chat.view.actions

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.domain.entity.chat.messages.meta.LocationMessage
import mega.privacy.android.domain.entity.chat.messages.normal.NormalMessage
import mega.privacy.android.domain.entity.chat.messages.normal.TextMessage
import mega.privacy.mobile.analytics.event.ChatConversationEditActionMenuEvent
import mega.privacy.mobile.analytics.event.ChatConversationEditActionMenuItemEvent
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import test.mega.privacy.android.app.AnalyticsTestRule

@RunWith(AndroidJUnit4::class)
class EditMessageActionTest {

    val composeTestRule = createAndroidComposeRule<ComponentActivity>()
    private val analyticsRule = AnalyticsTestRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(analyticsRule).around(composeTestRule)

    private lateinit var underTest: EditMessageAction

    private val chatViewModel = mock<ChatViewModel>()

    @Before
    fun setUp() {
        underTest = EditMessageAction(
            chatViewModel = chatViewModel,
        )
    }

    @Test
    fun `test that action applies to editable, non location messages`() {
        assertThat(underTest.appliesTo(setOf(mock<NormalMessage> {
            on { isEditable } doReturn true
        }))).isTrue()
    }

    @Test
    fun `test that action does not apply to editable, location messages`() {
        assertThat(underTest.appliesTo(setOf(mock<LocationMessage> {
            on { isEditable } doReturn true
        }))).isFalse()
    }

    @Test
    fun `test that action does not apply to non editable messages`() {
        assertThat(underTest.appliesTo(setOf(mock<NormalMessage> {
            on { isEditable } doReturn false
        }))).isFalse()
    }

    @Test
    fun `test that composable contains edit bottom action`() {
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
    fun `test that on trigger invokes view model`() {
        val message = mock<LocationMessage> {
            on { isEditable } doReturn true
        }
        val messages = setOf(message)
        composeTestRule.setContent {
            underTest.OnTrigger(messages = messages) {
            }
        }

        verify(chatViewModel).onEditMessage(messages.first())
    }

    @Test
    fun `test that analytics tracker sends the right event when message action is triggered from a bottom sheet`() {
        underTest.trackTriggerEvent(source = MessageAction.TriggerSource.BottomSheet)

        assertThat(analyticsRule.events).contains(ChatConversationEditActionMenuItemEvent)
    }

    @Test
    fun `test that analytics tracker sends the right event when message action is triggered from a toolbar`() {
        underTest.trackTriggerEvent(source = MessageAction.TriggerSource.Toolbar)

        assertThat(analyticsRule.events).contains(ChatConversationEditActionMenuEvent)
    }
}