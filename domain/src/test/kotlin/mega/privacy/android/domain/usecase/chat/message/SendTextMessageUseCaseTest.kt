package mega.privacy.android.domain.usecase.chat.message

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.reset
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SendTextMessageUseCaseTest {

    private lateinit var underTest: SendTextMessageUseCase

    private val chatRepository = mock<ChatRepository>()
    private val createNormalChatMessageUseCase = mock<CreateNormalChatMessageUseCase>()

    @BeforeEach
    fun setup() {
        underTest = SendTextMessageUseCase(chatRepository, createNormalChatMessageUseCase)
    }

    @AfterEach
    fun resetMocks() {
        reset(chatRepository, createNormalChatMessageUseCase)
    }

    @Test
    fun `test that repository is invoked`() = runTest {
        val chatId = 123L
        val content = "content"
        whenever(chatRepository.sendMessage(chatId, content)).thenReturn(mock())
        underTest.invoke(chatId, content)
        verify(chatRepository).sendMessage(chatId, content)
    }
}