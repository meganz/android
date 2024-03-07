package mega.privacy.android.app.presentation.meeting.chat.view.actions

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.chat.messages.meta.GiphyMessage
import mega.privacy.android.domain.entity.chat.messages.meta.LocationMessage
import mega.privacy.android.domain.entity.chat.messages.meta.RichPreviewMessage
import mega.privacy.android.domain.entity.chat.messages.normal.NormalMessage
import mega.privacy.mobile.analytics.event.ChatConversationCopyActionMenuEvent
import mega.privacy.mobile.analytics.event.ChatConversationCopyActionMenuItemEvent
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import test.mega.privacy.android.app.AnalyticsTestRule

@RunWith(AndroidJUnit4::class)
class CopyMessageActionTest {
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()
    private val analyticsRule = AnalyticsTestRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(analyticsRule).around(composeTestRule)

    private lateinit var underTest: CopyMessageAction

    @Before
    fun setUp() {
        underTest = CopyMessageAction()
    }

    @Test
    fun `test that action applies to NormalMessage`() {
        assertThat(underTest.appliesTo(setOf(mock<NormalMessage>()))).isTrue()
    }

    @Test
    fun `test that action applies to LocationMessage`() {
        assertThat(underTest.appliesTo(setOf(mock<LocationMessage>()))).isTrue()
    }

    @Test
    fun `test that action applies to RichPreviewMessage`() {
        assertThat(underTest.appliesTo(setOf(mock<RichPreviewMessage>()))).isTrue()
    }

    @Test
    fun `test that action does not apply to other types of messages`() {
        assertThat(underTest.appliesTo(setOf(mock<GiphyMessage>()))).isFalse()
    }

    @Test
    fun `test that action does not apply to mixed types of messages`() {
        assertThat(
            underTest.appliesTo(
                setOf(
                    mock<NormalMessage>(),
                    mock<GiphyMessage>()
                )
            )
        ).isFalse()
    }

    @Test
    fun `test that composable contains bottom sheet option`() {
        val bottomSheetMenuItem = underTest.bottomSheetMenuItem(
            messages = setOf(mock<NormalMessage>()),
            hideBottomSheet = {},
            setAction = {},
        )
        composeTestRule.setContent { bottomSheetMenuItem() }
        composeTestRule.onNodeWithTag(underTest.bottomSheetItemTestTag).assertExists()
    }

    @Test
    fun `test that onHandled() is invoked when action is clicked`() {
        val onHandled: () -> Unit = mock()
        composeTestRule.setContent {
            underTest.OnTrigger(messages = setOf(mock<NormalMessage>()), onHandled = onHandled)
        }
        verify(onHandled).invoke()
    }

    @Test
    fun `test that analytics tracker sends the right event when message action is triggered from a bottom sheet`() {
        underTest.trackTriggerEvent(source = MessageAction.TriggerSource.BottomSheet)

        assertThat(analyticsRule.events).contains(ChatConversationCopyActionMenuItemEvent)
    }

    @Test
    fun `test that analytics tracker sends the right event when message action is triggered from a toolbar`() {
        underTest.trackTriggerEvent(source = MessageAction.TriggerSource.Toolbar)

        assertThat(analyticsRule.events).contains(ChatConversationCopyActionMenuEvent)
    }
}