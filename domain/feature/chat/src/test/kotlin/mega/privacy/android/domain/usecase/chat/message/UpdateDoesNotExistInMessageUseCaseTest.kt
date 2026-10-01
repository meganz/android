package mega.privacy.android.domain.usecase.chat.message

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UpdateDoesNotExistInMessageUseCaseTest {

    private lateinit var underTest: UpdateDoesNotExistInMessageUseCase

    private val chatMessageRepository = mock<ChatMessageRepository>()

    @BeforeEach
    fun setup() {
        underTest = UpdateDoesNotExistInMessageUseCase(
            chatMessageRepository = chatMessageRepository,
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(chatMessageRepository)
    }

    @Test
    fun `test that update node exists in message invokes correctly`() = runTest {
        val chatId = 123L
        val msgId = 456L
        whenever(chatMessageRepository.updateDoesNotExistInMessage(chatId, msgId)).thenReturn(Unit)
        underTest.invoke(chatId, msgId)
    }
}