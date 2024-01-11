package mega.privacy.android.domain.usecase.chat.message

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

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
    internal fun `test that map use case is called with the correct values`() = runTest {
        val expectedNextMessageUserHandle = 123L
        val expectedUserHandle = 456L
        val expectedMessageList = emptyList<ChatMessage>()
        chatRepository.stub {
            onBlocking { getMyUserHandle() } doReturn expectedUserHandle
        }
        getMessageListUseCase.stub {
            onBlocking { invoke(any()) } doReturn expectedMessageList
        }

        underTest.invoke(mock(), expectedNextMessageUserHandle)

        verify(mapChatMessageListUseCase).invoke(
            expectedMessageList, expectedUserHandle,
            expectedNextMessageUserHandle
        )
    }
}