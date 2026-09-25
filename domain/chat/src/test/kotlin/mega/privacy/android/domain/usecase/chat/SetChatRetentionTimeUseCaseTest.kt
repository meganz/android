package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.ChatRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetChatRetentionTimeUseCaseTest {

    private lateinit var underTest: SetChatRetentionTimeUseCase

    private val chatRepository: ChatRepository = mock()

    @BeforeEach
    fun setup() {
        underTest = SetChatRetentionTimeUseCase(chatRepository)
    }

    @Test
    fun `test that the chat's retention time is set with the correct chat ID and period`() =
        runTest {
            val chatID = 123L
            val period = 321L
            underTest(chatID, period)

            verify(chatRepository).setChatRetentionTime(chatID, period)
        }

    @AfterEach
    fun tearDown() {
        reset(chatRepository)
    }
}
