package mega.privacy.android.domain.usecase.chat.message

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClearAllChatDataUseCaseTest {
    private lateinit var underTest: ClearAllChatDataUseCase

    private val chatMessageRepository = mock<ChatMessageRepository>()

    @BeforeEach
    fun setUp() {
        underTest = ClearAllChatDataUseCase(chatMessageRepository)
    }

    @Test
    fun `invoke should call clearAllData from chatMessageRepository`() = runTest {
        underTest()
        verify(chatMessageRepository).clearAllData()
    }
}