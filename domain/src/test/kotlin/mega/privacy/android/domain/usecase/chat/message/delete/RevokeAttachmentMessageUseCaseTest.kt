package mega.privacy.android.domain.usecase.chat.message.delete

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RevokeAttachmentMessageUseCaseTest {

    private lateinit var underTest: RevokeAttachmentMessageUseCase

    private val chatMessageRepository = mock<ChatMessageRepository>()

    @BeforeAll
    fun setUp() {
        underTest = RevokeAttachmentMessageUseCase(chatMessageRepository)
    }

    @AfterEach
    fun resetMocks() {
        reset(chatMessageRepository)
    }

    @Test
    fun `test that revoke attachment message invokes and returns correctly`() = runTest {
        val chatId = 1L
        val msgId = 2L
        val chatMessage = mock<ChatMessage>()
        whenever(chatMessageRepository.revokeAttachmentMessage(chatId, msgId))
            .thenReturn(chatMessage)
        assertThat(underTest.invoke(chatId, msgId)).isEqualTo(chatMessage)
        verify(chatMessageRepository).revokeAttachmentMessage(eq(chatId), eq(msgId))
    }
}