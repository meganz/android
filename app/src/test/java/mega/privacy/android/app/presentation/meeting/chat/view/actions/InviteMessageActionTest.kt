package mega.privacy.android.app.presentation.meeting.chat.view.actions

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.presentation.meeting.chat.model.ChatViewModel
import mega.privacy.android.domain.entity.chat.messages.ContactAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.normal.NormalMessage
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class InviteMessageActionTest {

    @get:Rule
    var composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var underTest: InviteMessageAction

    private val chatViewModel = mock<ChatViewModel>()

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
    fun `test that onHandled() is invoked when action is clicked`() {
        val onHandled: () -> Unit = mock()
        composeTestRule.setContent {
            underTest.OnTrigger(
                messages = setOf(mock<ContactAttachmentMessage>()),
                onHandled = onHandled
            )
        }
        verify(onHandled).invoke()
    }

    @Test
    fun `test that viewmodel inviteContacts() is invoked when action is clicked`() {
        val contactAttachmentMessage = mock<ContactAttachmentMessage>()
        whenever(contactAttachmentMessage.isMe).thenReturn(false)
        whenever(contactAttachmentMessage.isContact).thenReturn(true)
        whenever(contactAttachmentMessage.userHandle).thenReturn(1L)
        val expectedMessageSet = setOf(contactAttachmentMessage)
        composeTestRule.setContent {
            underTest.OnTrigger(
                messages = expectedMessageSet,
                onHandled = {}
            )
        }

        verify(chatViewModel).inviteContacts(expectedMessageSet)
    }


}