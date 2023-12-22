package mega.privacy.android.domain.usecase.chat.message

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.ChatMessageType
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.invalid.FormatInvalidMessage
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub


class MapChatMessageListUseCaseTest {
    private lateinit var underTest: MapChatMessageListUseCase

    private val createTypedMessageUseCase = mock<CreateTypedMessageUseCase>()
    private val map = mapOf(ChatMessageType.NORMAL to createTypedMessageUseCase)
    private val createInvalidMessageUseCase = mock<CreateInvalidMessageUseCase>()

    @BeforeEach
    internal fun initUseCase() {
        underTest = MapChatMessageListUseCase(
            createTypedMessageUseCases = map,
            createInvalidMessageUseCase = createInvalidMessageUseCase
        )
    }

    @Test
    fun `test that messages with matching types are mapped`() {
        val message = mock<ChatMessage> {
            on { type } doReturn ChatMessageType.NORMAL
            on { userHandle } doReturn 123L
        }
        val list = listOf(message)

        val expectedMessage = mock<TypedMessage>()
        createTypedMessageUseCase.stub {
            on { invoke(message, true) } doReturn expectedMessage
        }

        val result = underTest(list, 123L)

        assertThat(result).containsExactly(expectedMessage)
    }

    @Test
    fun `test that messages with non-matching types are mapped to invalid messages`() {
        val message = mock<ChatMessage> {
            on { type } doReturn ChatMessageType.UNKNOWN
            on { userHandle } doReturn 123L
        }

        val invalidMessage = mock<FormatInvalidMessage>()
        createInvalidMessageUseCase.stub {
            on { invoke(message, true) } doReturn invalidMessage
        }

        val list = listOf(message)

        val result = underTest(list, 123L)

        assertThat(result).hasSize(1)
        assertThat(result.first()).isEqualTo(invalidMessage)
    }
}