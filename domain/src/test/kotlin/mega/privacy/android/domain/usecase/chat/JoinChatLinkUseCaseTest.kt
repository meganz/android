package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.Mockito.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JoinChatLinkUseCaseTest {

    lateinit var underTest: JoinChatLinkUseCase

    private val chatRepository: ChatRepository = mock()

    @BeforeAll
    fun setup() {
        underTest = JoinChatLinkUseCase(chatRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(chatRepository)
    }

    @Test
    fun `test that join chat link calls repository`() = runTest {
        val chatId = 123L
        underTest(chatId)
        verify(chatRepository).autojoinPublicChat(chatId)
    }
}