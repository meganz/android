package mega.privacy.android.app.presentation.meeting.chat.view.actions

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.domain.entity.chat.messages.ContactAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.PendingFileAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.normal.NormalMessage
import mega.privacy.mobile.analytics.event.ChatConversationResumeTransfersMenuItemEvent
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import test.mega.privacy.android.app.AnalyticsTestRule

@RunWith(AndroidJUnit4::class)
class ResumeTransfersMessageActionTest {
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()
    private val analyticsRule = AnalyticsTestRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(analyticsRule).around(composeTestRule)

    private lateinit var underTest: ResumeTransfersMessageAction

    private val chatViewModel = mock<ChatViewModel>()

    private val contactMessage = mock<ContactAttachmentMessage> {
        on { isContact } doReturn true
    }

    @Before
    fun setUp() {
        underTest = ResumeTransfersMessageAction(
            chatViewModel = chatViewModel,
        )
    }

    @Test
    fun `test that action applies to not sent messages`() {
        whenever(chatViewModel.areTransfersPaused()).thenReturn(true)

        assertThat(underTest.appliesTo(setOf(mock<PendingFileAttachmentMessage> {
            on { isNotSent() } doReturn true
        }))).isTrue()
    }

    @Test
    fun `test that action does not apply to sent messages`() {
        whenever(chatViewModel.areTransfersPaused()).thenReturn(true)

        assertThat(underTest.appliesTo(setOf(mock<PendingFileAttachmentMessage> {
            on { isNotSent() } doReturn false
        }))).isFalse()
    }

    @Test
    fun `test that action does not apply to non pending attachment messages`() {
        whenever(chatViewModel.areTransfersPaused()).thenReturn(true)

        assertThat(underTest.appliesTo(setOf(mock<NormalMessage>()))).isFalse()
    }

    @Test
    fun `test that action does not apply when transfers are not paused`() {
        whenever(chatViewModel.areTransfersPaused()).thenReturn(false)

        assertThat(underTest.appliesTo(setOf(mock<PendingFileAttachmentMessage> {
            on { isNotSent() } doReturn true
        }))).isFalse()
    }

    @Test
    fun `test that composable contains resume transfers bottom action`() {
        composeTestRule.setContent(
            underTest.bottomSheetMenuItem(
                messages = setOf(mock<PendingFileAttachmentMessage> {
                    on { isNotSent() } doReturn true
                }),
                hideBottomSheet = {},
                setAction = {},
            )
        )

        composeTestRule.onNodeWithTag(underTest.bottomSheetItemTestTag).assertExists()
    }

    @Test
    fun `test that resume transfers is invoked on trigger`() {
        composeTestRule.setContent {
            underTest.OnTrigger(messages = emptySet()) {}
        }

        verify(chatViewModel).resumeTransfers()
    }

    @Test
    fun `test that analytics event is sent when action is clicked`() {
        val onHandled: () -> Unit = mock()
        composeTestRule.setContent {
            underTest.OnTrigger(messages = setOf(contactMessage), onHandled = onHandled)
        }
        verify(onHandled).invoke()
        assertThat(analyticsRule.events).contains(ChatConversationResumeTransfersMenuItemEvent)
    }
}