package mega.privacy.android.app.presentation.meeting.chat.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.presentation.meeting.chat.model.messages.UiChatMessage
import mega.privacy.android.app.presentation.meeting.chat.model.messages.header.ChatUnreadHeaderMessage
import mega.privacy.android.app.presentation.meeting.chat.model.messages.header.DateHeaderUiMessage
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.time.LocalDateTime
import java.time.ZoneOffset

class ChatMessageDateSeparatorMapperTest {
    private val underTest = ChatMessageDateSeparatorMapper()

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
    fun `test that date header is returned if first message is null`() {
        val secondMessage = mock<UiChatMessage> {
            on { timeSent }.thenReturn(123L)
        }
        assertThat(underTest(null, secondMessage)).isInstanceOf(DateHeaderUiMessage::class.java)
    }

    @Test
    fun `test that date header is returned if dates differ by a day`() {
        val firstDate = LocalDateTime.of(2023, 1, 1, 1, 1)
        val secondDate = firstDate.plusDays(1)

        val firstMessage = mock<UiChatMessage> {
            on { timeSent }.thenReturn(firstDate.toEpochSecond(ZoneOffset.UTC))
        }

        val secondMessage = mock<UiChatMessage> {
            on { timeSent }.thenReturn(secondDate.toEpochSecond(ZoneOffset.UTC))
        }

        assertThat(
            underTest(
                firstMessage,
                secondMessage
            )
        ).isInstanceOf(DateHeaderUiMessage::class.java)
    }

    @Test
    fun `test that null is returned if dates fall on the same day`() {
        val firstDate = LocalDateTime.of(2023, 1, 1, 1, 1)
        val secondDate = firstDate.plusHours(22)

        val firstMessage = mock<UiChatMessage> {
            on { timeSent }.thenReturn(firstDate.toEpochSecond(ZoneOffset.UTC))
        }

        val secondMessage = mock<UiChatMessage> {
            on { timeSent }.thenReturn(secondDate.toEpochSecond(ZoneOffset.UTC))
        }

        assertThat(
            underTest(
                firstMessage,
                secondMessage
            )
        ).isNull()
    }
}