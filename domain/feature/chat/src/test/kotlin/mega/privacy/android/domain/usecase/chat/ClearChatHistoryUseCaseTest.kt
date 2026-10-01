package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ClearChatHistoryUseCaseTest {

    private lateinit var underTest: ClearChatHistoryUseCase

    private val chatRepository = mock<ChatRepository>()

    @BeforeAll
    fun setup() {
        underTest = ClearChatHistoryUseCase(chatRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(chatRepository)
    }

    @Test
    fun `test that clear chat history invokes repository`() = runTest {
        val chatId = 123L
        underTest(chatId)
        verify(chatRepository).clearChatHistory(chatId)
        verifyNoMoreInteractions(chatRepository)
    }
}