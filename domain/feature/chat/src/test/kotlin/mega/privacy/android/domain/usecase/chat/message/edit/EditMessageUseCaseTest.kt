package mega.privacy.android.domain.usecase.chat.message.edit

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EditMessageUseCaseTest {

    private lateinit var underTest: EditMessageUseCase

    private val chatMessageRepository = mock<ChatMessageRepository>()

    @BeforeEach
    fun setup() {
        underTest = EditMessageUseCase(chatMessageRepository)
    }

    @AfterEach
    fun resetMocks() {
        reset(chatMessageRepository)
    }

    @Test
    fun `test that edit message use case invokes and returns correctly`() = runTest {
        val chatMessage = mock<ChatMessage>()
        val chatId = 1L
        val msgId = 2L
        val content = "content"
        whenever(chatMessageRepository.editMessage(chatId, msgId, content)).thenReturn(chatMessage)
        Truth.assertThat(underTest(chatId, msgId, content)).isEqualTo(chatMessage)
    }
}