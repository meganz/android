package mega.privacy.android.domain.usecase.chat.message

import com.google.common.truth.Truth
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

class FetchMessagePageUseCaseTest {
    private lateinit var underTest: FetchMessagePageUseCase

    private val chatRepository = mock<ChatRepository>()
    private val getMessageListUseCase = mock<GetMessageListUseCase>()
    private val mapChatMessageListUseCase = mock<MapChatMessageListUseCase>()

    @BeforeEach
    internal fun initUseCase() {
        underTest = FetchMessagePageUseCase(
            chatRepository = chatRepository,
            getMessageListUseCase = getMessageListUseCase,
            mapChatMessageListUseCase = mapChatMessageListUseCase,
        )
    }

    @Test
    fun `test that messages are returned in order from earliest to latest`() = runTest {
        val currentUserHandle = 456L
        chatRepository.stub {
            onBlocking { getMyUserHandle() } doReturn currentUserHandle
        }
        val chatMessages = listOf(mock<ChatMessage>())
        val typedMessages = (10L downTo 1L)
            .map { value ->
                mock<TypedMessage> {
                    on { time } doReturn value
                }
            }

        getMessageListUseCase.stub {
            onBlocking { invoke(any()) } doReturn chatMessages
        }

        mapChatMessageListUseCase.stub {
            on { invoke(chatMessages, currentUserHandle) } doReturn typedMessages
        }

        val result = underTest(emptyFlow())

        Truth.assertThat(result.map { it.time }).isInOrder()
    }
}