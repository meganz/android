package mega.privacy.android.app.presentation.meeting.chat.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.presentation.meeting.chat.mapper.ChatMessageTimeSeparatorMapper
import mega.privacy.android.app.presentation.meeting.chat.model.messages.CallUiMessage
import mega.privacy.android.app.presentation.meeting.chat.model.messages.UiChatMessage
import mega.privacy.android.app.presentation.meeting.chat.model.messages.header.ChatUnreadHeaderMessage
import mega.privacy.android.app.presentation.meeting.chat.model.messages.header.TimeHeaderUiMessage
import mega.privacy.android.app.presentation.meeting.chat.model.messages.management.ManagementUiChatMessage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatMessageTimeSeparatorMapperTest {
    private val underTest = ChatMessageTimeSeparatorMapper()

    @Test
    fun `test that null is returned if next message is null`() {
        assertThat(underTest(null, null)).isNull()
    }

    @Test
    fun `test that null is returned if next message time is null`() {
        assertThat(underTest(null, null)).isNull()
    }

    @Test
    fun `test that null is returned if next message is a header message`() {
        val firstMessage = mock<UiChatMessage> {
            on { timeSent }.thenReturn(123L)
        }
        val secondMessage = mock<ChatUnreadHeaderMessage>()
        assertThat(underTest(firstMessage, secondMessage)).isNull()
    }

    @Test
    fun `test that time header is returned if first message is null`() {
        val secondMessage = mock<UiChatMessage> {
            on { timeSent }.thenReturn(123L)
            on { id }.thenReturn(1L)
            on { displayAsMine }.thenReturn(false)
            on { userHandle }.thenReturn(345L)
        }
        val result = underTest(null, secondMessage)
        assertThat(result).isInstanceOf(TimeHeaderUiMessage::class.java)
        assertThat(result?.id).isEqualTo(1L)
        assertThat(result?.timeSent).isEqualTo(123L)
    }

    @Test
    fun `test that time header is returned if second message is management message`() {
        val firstMessage = mock<UiChatMessage> {
            on { timeSent }.thenReturn(123L)
            on { id }.thenReturn(1L)
            on { displayAsMine }.thenReturn(false)
            on { userHandle }.thenReturn(345L)
        }
        val secondMessage = mock<ManagementUiChatMessage> {
            on { timeSent }.thenReturn(456L)
            on { id }.thenReturn(2L)
            on { displayAsMine }.thenReturn(false)
            on { userHandle }.thenReturn(345L)
        }
        val result = underTest(firstMessage, secondMessage)
        assertThat(result).isInstanceOf(TimeHeaderUiMessage::class.java)
        assertThat(result?.id).isEqualTo(2L)
        assertThat(result?.displayAsMine).isEqualTo(false)
        assertThat(result?.userHandle).isEqualTo(-1L)
    }

    @Test
    fun `test that time header is returned if second message is call message`() {
        val secondMessage = mock<CallUiMessage> {
            on { timeSent }.thenReturn(456L)
            on { id }.thenReturn(2L)
            on { displayAsMine }.thenReturn(false)
            on { userHandle }.thenReturn(345L)
        }
        val result = underTest(null, secondMessage)
        assertThat(result).isInstanceOf(TimeHeaderUiMessage::class.java)
        assertThat(result?.id).isEqualTo(2L)
        assertThat(result?.displayAsMine).isEqualTo(false)
        assertThat(result?.userHandle).isEqualTo(-1L)
    }

    @Test
    fun `test that time header is returned if first message and second message are different user handle and same time sent`() {
        val firstMessage = mock<UiChatMessage> {
            on { timeSent }.thenReturn(123L)
            on { id }.thenReturn(1L)
            on { displayAsMine }.thenReturn(false)
            on { userHandle }.thenReturn(345L)
        }
        val secondMessage = mock<UiChatMessage> {
            on { timeSent }.thenReturn(123L)
            on { id }.thenReturn(2L)
            on { displayAsMine }.thenReturn(false)
            on { userHandle }.thenReturn(543L)
        }
        val result = underTest(firstMessage, secondMessage)
        assertThat(result).isInstanceOf(TimeHeaderUiMessage::class.java)
        assertThat(result?.id).isEqualTo(secondMessage.id)
        assertThat(result?.userHandle).isEqualTo(secondMessage.userHandle)
    }

    @Test
    fun `test that time header is returned if first message and second message are same user handle and different time sent`() {
        val handle = 345L
        val firstMessage = mock<UiChatMessage> {
            on { timeSent }.thenReturn(123L)
            on { id }.thenReturn(1L)
            on { displayAsMine }.thenReturn(false)
            on { userHandle }.thenReturn(handle)
        }
        val secondMessage = mock<UiChatMessage> {
            on { timeSent }.thenReturn(456L)
            on { id }.thenReturn(2L)
            on { displayAsMine }.thenReturn(false)
            on { userHandle }.thenReturn(handle)
        }
        val result = underTest(firstMessage, secondMessage)
        assertThat(result).isInstanceOf(TimeHeaderUiMessage::class.java)
        assertThat(result?.id).isEqualTo(2L)
        assertThat(result?.userHandle).isEqualTo(handle)
    }
}