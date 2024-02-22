package mega.privacy.android.domain.usecase.chat.message.delete

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DeleteGeneralMessageUseCaseTest {

    private lateinit var underTest: DeleteGeneralMessageUseCase

    private val chatMessageRepository = mock<ChatMessageRepository>()

    @BeforeAll
    fun setUp() {
        underTest = DeleteGeneralMessageUseCase(chatMessageRepository)
    }

    @AfterEach
    fun resetMocks() {
        reset(chatMessageRepository)
    }

    @Test
    fun `test that delete general message invokes correctly`() = runTest {
        val chatId = 1L
        val msgId = 2L
        val message = mock<TypedMessage> {
            on { this.chatId } doReturn chatId
            on { this.msgId } doReturn msgId
            on { this.isDeletable } doReturn true
        }
        val chatMessage = mock<ChatMessage>()
        whenever(chatMessageRepository.deleteMessage(chatId, msgId)).thenReturn(chatMessage)
        underTest.invoke(listOf(message))
        verify(chatMessageRepository).deleteMessage(eq(chatId), eq(msgId))
    }
}