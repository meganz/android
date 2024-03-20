package mega.privacy.android.app.presentation.meeting.chat.view.actions

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.domain.entity.chat.messages.ContactAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.NodeAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.PendingFileAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.VoiceClipMessage
import mega.privacy.android.domain.entity.chat.messages.invalid.InvalidMessage
import mega.privacy.android.domain.entity.chat.messages.management.ManagementMessage
import mega.privacy.android.domain.entity.chat.messages.meta.GiphyMessage
import mega.privacy.android.domain.entity.chat.messages.meta.InvalidMetaMessage
import mega.privacy.android.domain.entity.chat.messages.meta.LocationMessage
import mega.privacy.android.domain.entity.chat.messages.meta.RichPreviewMessage
import mega.privacy.android.domain.entity.chat.messages.normal.NormalMessage
import mega.privacy.mobile.analytics.event.ChatConversationSelectActionMenuItemEvent
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import test.mega.privacy.android.app.AnalyticsTestRule

@RunWith(AndroidJUnit4::class)
class SelectMessageActionTest {

    val composeTestRule = createAndroidComposeRule<ComponentActivity>()
    private val analyticsRule = AnalyticsTestRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(analyticsRule).around(composeTestRule)

    private lateinit var underTest: SelectMessageAction

    private val chatViewModel: ChatViewModel = mock()

    @Before
    fun setUp() {
        underTest = SelectMessageAction(chatViewModel)
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
    fun `test that action applies to GiphyMessage`() {
        assertThat(underTest.appliesTo(setOf(mock<GiphyMessage>()))).isTrue()
    }

    @Test
    fun `test that action applies to NodeAttachmentMessage`() {
        assertThat(underTest.appliesTo(setOf(mock<NodeAttachmentMessage>()))).isTrue()
    }

    @Test
    fun `test that action applies to VoiceClipMessage`() {
        assertThat(underTest.appliesTo(setOf(mock<VoiceClipMessage>()))).isTrue()
    }

    @Test
    fun `test that action applies to ContactAttachmentMessage`() {
        assertThat(underTest.appliesTo(setOf(mock<ContactAttachmentMessage>()))).isTrue()
    }

    @Test
    fun `test that action does not apply to ManagementMessage`() {
        assertThat(underTest.appliesTo(setOf(mock<ManagementMessage>()))).isFalse()
    }

    @Test
    fun `test that action does not apply to InvalidMessage`() {
        assertThat(underTest.appliesTo(setOf(mock<InvalidMessage>()))).isFalse()
    }

    @Test
    fun `test that action does not apply to InvalidMetaMessage`() {
        assertThat(underTest.appliesTo(setOf(mock<InvalidMetaMessage>()))).isFalse()
    }

    @Test
    fun `test that action does not apply to more than one message`() {
        assertThat(underTest.appliesTo(setOf(mock<NormalMessage>(), mock<NormalMessage>())))
            .isFalse()
    }

    @Test
    fun `test that action applies to pending messages`() {
        assertThat(underTest.appliesTo(setOf(mock<PendingFileAttachmentMessage>()))).isTrue()
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
    fun `test that view model is invoked with onEnableSelectMode when action is clicked`() {
        val onHandled: () -> Unit = mock()
        composeTestRule.setContent {
            underTest.OnTrigger(messages = setOf(mock<NormalMessage>()), onHandled = onHandled)
        }
        verify(chatViewModel).onEnableSelectMode()
    }

    @Test
    fun `test that onHandled() is invoked when action is clicked`() {
        val onHandled: () -> Unit = mock()
        composeTestRule.setContent {
            underTest.OnTrigger(messages = setOf(mock<NormalMessage>()), onHandled = onHandled)
        }
        verify(onHandled).invoke()
        assertThat(analyticsRule.events).contains(ChatConversationSelectActionMenuItemEvent)
    }
}