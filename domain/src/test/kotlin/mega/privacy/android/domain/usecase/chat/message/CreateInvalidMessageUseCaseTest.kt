package mega.privacy.android.domain.usecase.chat.message

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.ChatMessageCode
import mega.privacy.android.domain.entity.chat.ChatMessageStatus
import mega.privacy.android.domain.entity.chat.ChatMessageType
import mega.privacy.android.domain.entity.chat.messages.invalid.FormatInvalidMessage
import mega.privacy.android.domain.entity.chat.messages.invalid.SignatureInvalidMessage
import mega.privacy.android.domain.entity.chat.messages.invalid.UnrecognizableInvalidMessage
import mega.privacy.android.domain.entity.chat.messages.request.CreateTypedMessageRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class CreateInvalidMessageUseCaseTest {
    private lateinit var underTest: CreateInvalidMessageUseCase

    @BeforeEach
    internal fun setUp() {
        underTest = CreateInvalidMessageUseCase()
    }

    @Test
    fun `test that invalid type returns unrecognisable message`() = runTest {
        val message = mock<ChatMessage> {
            on { messageId }.thenReturn(123L)
            on { timestamp }.thenReturn(123L)
            on { userHandle }.thenReturn(123L)
            on { type }.thenReturn(ChatMessageType.INVALID)
            on { status } doReturn ChatMessageStatus.UNKNOWN
        }
        assertThat(
            underTest.invoke(
                CreateTypedMessageRequest(
                    chatMessage = message,
                    chatId = 123L,
                    isMine = true,
                    reactions = emptyList(),
                    exists = true,
                )
            )
        ).isInstanceOf(UnrecognizableInvalidMessage::class.java)
    }

    @Test
    fun `test that invalid format code returns invalid format message`() = runTest {
        val message = mock<ChatMessage> {
            on { messageId }.thenReturn(123L)
            on { timestamp }.thenReturn(123L)
            on { userHandle }.thenReturn(123L)
            on { type }.thenReturn(ChatMessageType.NORMAL)
            on { code }.thenReturn(ChatMessageCode.INVALID_FORMAT)
            on { status } doReturn ChatMessageStatus.UNKNOWN
        }
        assertThat(
            underTest.invoke(
                CreateTypedMessageRequest(
                    chatMessage = message,
                    chatId = 123L,
                    isMine = true,
                    reactions = emptyList(),
                    exists = true,
                )
            )
        ).isInstanceOf(FormatInvalidMessage::class.java)
    }

    @Test
    fun `test that invalid signature code returns invalid signature message`() = runTest {
        val message = mock<ChatMessage> {
            on { messageId }.thenReturn(123L)
            on { timestamp }.thenReturn(123L)
            on { userHandle }.thenReturn(123L)
            on { type }.thenReturn(ChatMessageType.NORMAL)
            on { code }.thenReturn(ChatMessageCode.INVALID_SIGNATURE)
            on { status } doReturn ChatMessageStatus.UNKNOWN
        }
        assertThat(
            underTest.invoke(
                CreateTypedMessageRequest(
                    chatMessage = message,
                    chatId = 123L,
                    isMine = true,
                    reactions = emptyList(),
                    exists = true,
                )
            )
        ).isInstanceOf(SignatureInvalidMessage::class.java)
    }

    @Test
    fun `test that unrecognisable message is returned as default`() = runTest {
        val message = mock<ChatMessage> {
            on { messageId }.thenReturn(123L)
            on { timestamp }.thenReturn(123L)
            on { userHandle }.thenReturn(123L)
            on { type }.thenReturn(ChatMessageType.NORMAL)
            on { code }.thenReturn(ChatMessageCode.INVALID_KEY)
            on { status } doReturn ChatMessageStatus.UNKNOWN
        }
        assertThat(
            underTest.invoke(
                CreateTypedMessageRequest(
                    chatMessage = message,
                    chatId = 123L,
                    isMine = true,
                    reactions = emptyList(),
                    exists = true,
                )
            )
        ).isInstanceOf(UnrecognizableInvalidMessage::class.java)
    }
}