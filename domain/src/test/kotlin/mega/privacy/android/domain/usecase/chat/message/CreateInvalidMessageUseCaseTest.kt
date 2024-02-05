package mega.privacy.android.domain.usecase.chat.message

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.ChatMessageCode
import mega.privacy.android.domain.entity.chat.ChatMessageType
import mega.privacy.android.domain.entity.chat.messages.invalid.FormatInvalidMessage
import mega.privacy.android.domain.entity.chat.messages.invalid.SignatureInvalidMessage
import mega.privacy.android.domain.entity.chat.messages.invalid.UnrecognizableInvalidMessage
import mega.privacy.android.domain.entity.chat.messages.request.CreateTypedMessageRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

class CreateInvalidMessageUseCaseTest {
    private lateinit var underTest: CreateInvalidMessageUseCase

    @BeforeEach
    internal fun setUp() {
        underTest = CreateInvalidMessageUseCase()
    }

    @Test
    fun `test that invalid type returns unrecognisable message`() {
        val message = mock<ChatMessage> {
            on { msgId }.thenReturn(123L)
            on { timestamp }.thenReturn(123L)
            on { userHandle }.thenReturn(123L)
            on { type }.thenReturn(ChatMessageType.INVALID)
        }
        assertThat(
            underTest.invoke(
                CreateTypedMessageRequest(
                    chatMessage = message,
                    isMine = true,
                    shouldShowAvatar = true,
                    shouldShowTime = true,
                    shouldShowDate = true,
                    reactions = emptyList(),
                )
            )
        ).isInstanceOf(UnrecognizableInvalidMessage::class.java)
    }

    @Test
    fun `test that invalid format code returns invalid format message`() {
        val message = mock<ChatMessage> {
            on { msgId }.thenReturn(123L)
            on { timestamp }.thenReturn(123L)
            on { userHandle }.thenReturn(123L)
            on { type }.thenReturn(ChatMessageType.NORMAL)
            on { code }.thenReturn(ChatMessageCode.INVALID_FORMAT)
        }
        assertThat(
            underTest.invoke(
                CreateTypedMessageRequest(
                    chatMessage = message,
                    isMine = true,
                    shouldShowAvatar = true,
                    shouldShowTime = true,
                    shouldShowDate = true,
                    reactions = emptyList(),
                )
            )
        ).isInstanceOf(FormatInvalidMessage::class.java)
    }

    @Test
    fun `test that invalid signature code returns invalid signature message`() {
        val message = mock<ChatMessage> {
            on { msgId }.thenReturn(123L)
            on { timestamp }.thenReturn(123L)
            on { userHandle }.thenReturn(123L)
            on { type }.thenReturn(ChatMessageType.NORMAL)
            on { code }.thenReturn(ChatMessageCode.INVALID_SIGNATURE)
        }
        assertThat(
            underTest.invoke(
                CreateTypedMessageRequest(
                    chatMessage = message,
                    isMine = true,
                    shouldShowAvatar = true,
                    shouldShowTime = true,
                    shouldShowDate = true,
                    reactions = emptyList(),
                )
            )
        ).isInstanceOf(SignatureInvalidMessage::class.java)
    }

    @Test
    fun `test that unrecognisable message is returned as default`() {
        val message = mock<ChatMessage> {
            on { msgId }.thenReturn(123L)
            on { timestamp }.thenReturn(123L)
            on { userHandle }.thenReturn(123L)
            on { type }.thenReturn(ChatMessageType.NORMAL)
            on { code }.thenReturn(ChatMessageCode.INVALID_KEY)
        }
        assertThat(
            underTest.invoke(
                CreateTypedMessageRequest(
                    chatMessage = message,
                    isMine = true,
                    shouldShowAvatar = true,
                    shouldShowTime = true,
                    shouldShowDate = true,
                    reactions = emptyList(),
                )
            )
        ).isInstanceOf(UnrecognizableInvalidMessage::class.java)
    }
}