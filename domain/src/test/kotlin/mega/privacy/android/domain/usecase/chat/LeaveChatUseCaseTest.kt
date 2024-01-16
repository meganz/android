package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LeaveChatUseCaseTest {

    private lateinit var underTest: LeaveChatUseCase

    private val chatRepository = mock<ChatRepository>()

    @BeforeAll
    internal fun setup() {
        underTest = LeaveChatUseCase(
            chatRepository = chatRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(chatRepository)
    }

    @Test
    fun `test that repository is invoked`() = runTest {
        val chatId = 1L
        underTest(chatId)
        verify(chatRepository).leaveChat(chatId)
    }
}