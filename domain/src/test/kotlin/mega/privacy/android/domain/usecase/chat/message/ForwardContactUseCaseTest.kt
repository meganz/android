package mega.privacy.android.domain.usecase.chat.message

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.messages.request.CreateTypedMessageRequest
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.reset
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ForwardContactUseCaseTest {

    private lateinit var underTest: ForwardContactUseCase

    private val chatRepository = mock<ChatRepository>()
    private val chatMessageRepository = mock<ChatMessageRepository>()
    private val createSaveSentMessageRequestUseCase = mock<CreateSaveSentMessageRequestUseCase>()

    private val sourceChatId = 123L
    private val msgId = 456L
    private val targetChatId = 789L

    @BeforeEach
    fun setup() {
        underTest = ForwardContactUseCase(
            chatRepository = chatRepository,
            chatMessageRepository = chatMessageRepository,
            createSaveSentMessageRequestUseCase = createSaveSentMessageRequestUseCase,
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            chatRepository,
            chatMessageRepository,
            createSaveSentMessageRequestUseCase,
        )
    }

    @Test
    fun `test that message is sent`() = runTest {
        whenever(chatMessageRepository.forwardContact(sourceChatId, msgId, targetChatId))
            .thenReturn(mock())
        underTest.invoke(sourceChatId, msgId, targetChatId)
        verify(chatMessageRepository).forwardContact(sourceChatId, msgId, targetChatId)
    }

    @Test
    fun `test that message is stored`() = runTest {
        val message = mock<ChatMessage>()
        val request = mock<CreateTypedMessageRequest>()
        whenever(chatMessageRepository.forwardContact(sourceChatId, msgId, targetChatId))
            .thenReturn(message)
        whenever(createSaveSentMessageRequestUseCase(message)).thenReturn(request)
        underTest.invoke(sourceChatId, msgId, targetChatId)
        verify(chatRepository).storeMessages(targetChatId, listOf(request))
    }
}