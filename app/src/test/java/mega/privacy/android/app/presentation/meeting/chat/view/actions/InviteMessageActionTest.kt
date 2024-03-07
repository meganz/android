package mega.privacy.android.app.presentation.meeting.chat.view.actions

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.app.presentation.meeting.chat.model.InviteUserAsContactResult
import mega.privacy.android.app.presentation.meeting.chat.model.messages.InviteMultipleUsersAsContactResult
import mega.privacy.android.app.presentation.meeting.chat.view.message.attachment.ContactAttachmentMessageViewModel
import mega.privacy.android.domain.entity.chat.messages.ContactAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.normal.NormalMessage
import mega.privacy.mobile.analytics.event.ChatConversationInviteActionMenuEvent
import mega.privacy.mobile.analytics.event.ChatConversationInviteActionMenuItemEvent
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import test.mega.privacy.android.app.AnalyticsTestRule

@RunWith(AndroidJUnit4::class)
class InviteMessageActionTest {

    val composeTestRule = createAndroidComposeRule<ComponentActivity>()
    private val analyticsRule = AnalyticsTestRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(analyticsRule).around(composeTestRule)

    private lateinit var underTest: InviteMessageAction

    private val chatViewModel = mock<ChatViewModel>()

    private val contactAttachmentMessageViewModel = mock<ContactAttachmentMessageViewModel>()

    private val viewModelStore = mock<ViewModelStore> {
        on { get(argThat<String> { contains(ContactAttachmentMessageViewModel::class.java.canonicalName.orEmpty()) }) } doReturn contactAttachmentMessageViewModel
    }
    private val viewModelStoreOwner = mock<ViewModelStoreOwner> {
        on { viewModelStore } doReturn viewModelStore
    }

    @Before
    fun setUp() {
        underTest = InviteMessageAction(chatViewModel)
    }


    @Test
    fun `test that action applies to ContactAttachmentMessage`() {
        assertThat(underTest.appliesTo(setOf(mock<ContactAttachmentMessage>()))).isTrue()
    }

    @Test
    fun `test that actions does not apply if messages are empty`() {
        assertThat(underTest.appliesTo(emptySet())).isFalse()
    }

    @Test
    fun `test that action does not apply to ContactAttachmentMessage with isContactMe true`() {
        val contactAttachmentMessage = mock<ContactAttachmentMessage>()
        whenever(contactAttachmentMessage.isMe).thenReturn(true)
        assertThat(underTest.appliesTo(setOf(contactAttachmentMessage))).isFalse()
    }

    @Test
    fun `test that action does not apply to ContactAttachmentMessage with isContact true and userHandle -1L`() {
        val contactAttachmentMessage = mock<ContactAttachmentMessage>()
        whenever(contactAttachmentMessage.isMe).thenReturn(false)
        whenever(contactAttachmentMessage.isContact).thenReturn(true)
        whenever(contactAttachmentMessage.userHandle).thenReturn(-1L)
        assertThat(underTest.appliesTo(setOf(contactAttachmentMessage))).isFalse()
    }

    @Test
    fun `test that action does not apply to other types of messages`() {
        assertThat(underTest.appliesTo(setOf(mock<NormalMessage>()))).isFalse()
    }

    @Test
    fun `test that action does not apply to mixed types of messages`() {
        val contactAttachmentMessage = mock<ContactAttachmentMessage>()
        whenever(contactAttachmentMessage.isMe).thenReturn(false)
        whenever(contactAttachmentMessage.isContact).thenReturn(true)
        whenever(contactAttachmentMessage.userHandle).thenReturn(1L)
        assertThat(
            underTest.appliesTo(
                setOf(
                    mock<NormalMessage>(),
                    contactAttachmentMessage
                )
            )
        ).isFalse()
    }

    @Test

    fun `test that onHandle is invoked when one contact attachment message is invited`() = runTest {
        val contactAttachmentMessage = mock<ContactAttachmentMessage> {
            on { isMe } doReturn false
            on { isContact } doReturn true
            on { userHandle } doReturn 1L
        }
        val onHandled = mock<() -> Unit>()
        whenever(contactAttachmentMessageViewModel.inviteContact(contactAttachmentMessage)).thenReturn(
            InviteUserAsContactResult.ContactInviteSent
        )
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides viewModelStoreOwner
            ) {
                underTest.OnTrigger(
                    messages = setOf(contactAttachmentMessage),
                    onHandled = onHandled
                )
            }
        }
        verify(onHandled).invoke()
    }

    @Test
    fun `test that onHandle is invoked when multi contact attachment messages are invited`() =
        runTest {
            val contactAttachmentMessage1 = mock<ContactAttachmentMessage> {
                on { isMe } doReturn false
                on { isContact } doReturn true
                on { userHandle } doReturn 1L
            }
            val contactAttachmentMessage2 = mock<ContactAttachmentMessage> {
                on { isMe } doReturn false
                on { isContact } doReturn true
                on { userHandle } doReturn 2L
            }
            val onHandled = mock<() -> Unit>()
            val expectedMessageSet = setOf(
                contactAttachmentMessage1,
                contactAttachmentMessage2
            )
            whenever(
                contactAttachmentMessageViewModel.inviteMultipleContacts(
                    expectedMessageSet
                )
            ).thenReturn(InviteMultipleUsersAsContactResult.AllSent(expectedMessageSet.size))
            composeTestRule.setContent {
                CompositionLocalProvider(
                    LocalViewModelStoreOwner provides viewModelStoreOwner
                ) {
                    underTest.OnTrigger(
                        messages = expectedMessageSet,
                        onHandled = onHandled
                    )
                }
            }
            verify(onHandled).invoke()
        }

    @Test
    fun `test that analytics tracker sends the right event when message action is triggered from a bottom sheet`() {
        underTest.trackTriggerEvent(source = MessageAction.TriggerSource.BottomSheet)

        assertThat(analyticsRule.events).contains(ChatConversationInviteActionMenuItemEvent)
    }

    @Test
    fun `test that analytics tracker sends the right event when message action is triggered from a toolbar`() {
        underTest.trackTriggerEvent(source = MessageAction.TriggerSource.Toolbar)

        assertThat(analyticsRule.events).contains(ChatConversationInviteActionMenuEvent)
    }
}