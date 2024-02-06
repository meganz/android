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
class AttachContactsUseCaseTest {

    private lateinit var underTest: AttachContactsUseCase

    private val chatRepository = mock<ChatRepository>()
    private val chatMessageRepository = mock<ChatMessageRepository>()
    private val createSaveSentMessageRequestUseCase = mock<CreateSaveSentMessageRequestUseCase>()

    private val chatId = 123L
    private val contactEmail1 = "contactEmail"
    private val contactEmail2 = "contactEmail2"
    private val sentMessage1 = mock<ChatMessage>()
    private val request1 = mock<CreateTypedMessageRequest>()

    @BeforeEach
    fun setup() {
        underTest = AttachContactsUseCase(
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
        whenever(chatMessageRepository.attachContact(chatId, contactEmail1)).thenReturn(mock())
        underTest.invoke(chatId, listOf(contactEmail1))
        verify(chatMessageRepository).attachContact(chatId, contactEmail1)
    }

    @Test
    fun `test that message is stored`() = runTest {
        whenever(chatMessageRepository.attachContact(chatId, contactEmail1))
            .thenReturn(sentMessage1)
        whenever(createSaveSentMessageRequestUseCase(sentMessage1)).thenReturn(request1)
        underTest.invoke(chatId, listOf(contactEmail1))
        verify(chatRepository).storeMessages(chatId, listOf(request1))
    }

    @Test
    fun `test that messages are sent`() = runTest {
        whenever(chatMessageRepository.attachContact(chatId, contactEmail1)).thenReturn(mock())
        whenever(chatMessageRepository.attachContact(chatId, contactEmail2)).thenReturn(mock())
        underTest.invoke(chatId, listOf(contactEmail1, contactEmail2))
        verify(chatMessageRepository).attachContact(chatId, contactEmail1)
        verify(chatMessageRepository).attachContact(chatId, contactEmail2)
    }

    @Test
    fun `test that messages are stored`() = runTest {
        val sentMessage2 = mock<ChatMessage>()
        whenever(chatMessageRepository.attachContact(chatId, contactEmail1))
            .thenReturn(sentMessage1)
        whenever(chatMessageRepository.attachContact(chatId, contactEmail2))
            .thenReturn(sentMessage2)
        val request2 = mock<CreateTypedMessageRequest>()
        whenever(createSaveSentMessageRequestUseCase(sentMessage1)).thenReturn(request1)
        whenever(createSaveSentMessageRequestUseCase(sentMessage2)).thenReturn(request2)
        underTest.invoke(chatId, listOf(contactEmail1, contactEmail2))
        verify(chatRepository).storeMessages(chatId, listOf(request1, request2))
    }
}