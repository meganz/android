package mega.privacy.android.domain.usecase.chat.message.delete

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.messages.NodeAttachmentMessage
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
class DeleteNodeAttachmentMessageUseCaseTest {

    private lateinit var underTest: DeleteNodeAttachmentMessageUseCase

    private val revokeAttachmentMessageUseCase = mock<RevokeAttachmentMessageUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = DeleteNodeAttachmentMessageUseCase(revokeAttachmentMessageUseCase)
    }

    @AfterEach
    fun resetMocks() {
        reset(revokeAttachmentMessageUseCase)
    }

    @Test
    fun `test that delete node attachment message invokes correctly`() = runTest {
        val chatId = 1L
        val msgId = 2L
        val message = mock<NodeAttachmentMessage> {
            on { this.chatId } doReturn chatId
            on { this.msgId } doReturn msgId
            on { this.isDeletable } doReturn true
        }
        val chatMessage = mock<ChatMessage>()
        whenever(revokeAttachmentMessageUseCase(chatId, msgId)).thenReturn(chatMessage)
        underTest.invoke(listOf(message))
        verify(revokeAttachmentMessageUseCase).invoke(eq(chatId), eq(msgId))
    }
}