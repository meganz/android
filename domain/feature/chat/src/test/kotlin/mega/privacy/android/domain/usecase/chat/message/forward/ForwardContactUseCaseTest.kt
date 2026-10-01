package mega.privacy.android.domain.usecase.chat.message.forward

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.messages.ContactAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.ForwardResult
import mega.privacy.android.domain.entity.chat.messages.normal.NormalMessage
import mega.privacy.android.domain.entity.chat.messages.request.CreateTypedMessageRequest
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import mega.privacy.android.domain.usecase.chat.message.CreateSaveSentMessageRequestUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.reset
import org.mockito.kotlin.doReturn
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
    private val message = mock<ContactAttachmentMessage> {
        on { this.chatId } doReturn sourceChatId
        on { this.msgId } doReturn msgId
    }
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
    fun `test that empty is returned if message is not a contact attachment`() = runTest {
        val message = mock<NormalMessage>()
        underTest.invoke(listOf(targetChatId), message)
        assertThat(underTest.invoke(listOf(targetChatId), message)).isEmpty()
    }

    @Test
    fun `test that success is returned if message is a contact attachment`() = runTest {
        underTest.invoke(listOf(targetChatId), message)
        assertThat(underTest.invoke(listOf(targetChatId), message))
            .isEqualTo(listOf(ForwardResult.Success(targetChatId)))
    }

    @Test
    fun `test that message is sent`() = runTest {
        whenever(chatMessageRepository.forwardContact(sourceChatId, msgId, targetChatId))
            .thenReturn(mock())
        underTest.invoke(listOf(targetChatId), message)
        verify(chatMessageRepository).forwardContact(sourceChatId, msgId, targetChatId)
    }

    @Test
    fun `test that message is stored`() = runTest {
        val chatMessage = mock<ChatMessage>()
        val request = mock<CreateTypedMessageRequest> {
            on { this.chatId } doReturn targetChatId
        }
        whenever(chatMessageRepository.forwardContact(sourceChatId, msgId, targetChatId))
            .thenReturn(chatMessage)
        whenever(createSaveSentMessageRequestUseCase(chatMessage, targetChatId)).thenReturn(request)
        underTest.invoke(listOf(targetChatId), message)
        verify(chatRepository).storeMessages(listOf(request))
    }
}