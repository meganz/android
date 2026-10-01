package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.stream.Stream
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetChatMessageUseCaseTest {

    private lateinit var underTest: GetChatMessageUseCase

    private val chatRepository = mock<ChatRepository>()

    private val chatMessage = mock<ChatMessage>()

    @BeforeAll
    fun setUp() {
        underTest = GetChatMessageUseCase(
            chatRepository = chatRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(chatRepository)
    }

    @ParameterizedTest(name = "{0} when getMessage is {1} and getMessageFromNodeHistory is {2}")
    @MethodSource("provideParameters")
    fun `test that ChatMessage is`(
        expectedMessage: ChatMessage?,
        message: ChatMessage?,
        messageFromHistory: ChatMessage?,
    ) = runTest {
        whenever(chatRepository.getMessage(any(), any())).thenReturn(message)
        whenever(chatRepository.getMessageFromNodeHistory(any(), any()))
            .thenReturn(messageFromHistory)

        assertEquals(underTest(any(), any()), expectedMessage)
    }

    private fun provideParameters(): Stream<Arguments?>? =
        Stream.of(
            Arguments.of(chatMessage, chatMessage, chatMessage),
            Arguments.of(chatMessage, chatMessage, null),
            Arguments.of(chatMessage, null, chatMessage),
            Arguments.of(null, null, null),
        )
}