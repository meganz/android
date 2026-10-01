package mega.privacy.android.domain.usecase.chat.message.delete

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.messages.VoiceClipMessage
import mega.privacy.android.domain.repository.FileSystemRepository
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
class DeleteVoiceClipMessageUseCaseTest {

    private lateinit var underTest: DeleteVoiceClipMessageUseCase

    private val fileSystemRepository = mock<FileSystemRepository>()
    private val revokeAttachmentMessageUseCase = mock<RevokeAttachmentMessageUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = DeleteVoiceClipMessageUseCase(
            fileSystemRepository,
            revokeAttachmentMessageUseCase,
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            fileSystemRepository,
            revokeAttachmentMessageUseCase,
        )
    }

    @Test
    fun `test that delete voice message invokes correctly`() = runTest {
        val chatId = 1L
        val msgId = 2L
        val name = "name"
        val message = mock<VoiceClipMessage> {
            on { this.chatId } doReturn chatId
            on { this.msgId } doReturn msgId
            on { this.name } doReturn name
            on { this.isDeletable } doReturn true
        }
        val chatMessage = mock<ChatMessage>()
        whenever(fileSystemRepository.deleteVoiceClip(name)).thenReturn(true)
        whenever(revokeAttachmentMessageUseCase(chatId, msgId)).thenReturn(chatMessage)
        underTest.invoke(listOf(message))
        verify(fileSystemRepository).deleteVoiceClip(name)
        verify(revokeAttachmentMessageUseCase).invoke(eq(chatId), eq(msgId))
    }
}