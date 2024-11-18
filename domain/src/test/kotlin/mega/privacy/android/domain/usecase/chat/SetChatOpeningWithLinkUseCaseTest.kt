package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetChatOpeningWithLinkUseCaseTest {
    private lateinit var underTest: SetChatOpeningWithLinkUseCase

    private val chatRepository = mock<ChatRepository>()

    @BeforeAll
    fun setup() {
        underTest = SetChatOpeningWithLinkUseCase(chatRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(chatRepository)
    }

    @Test
    fun `test that chat opening with link is set when invoked `() =
        runTest {
            val chatId = 123L
            underTest(chatId)
            verify(chatRepository).setChatOpeningWithLink(chatId)
        }
}
