package mega.privacy.android.domain.usecase.chat.message.paging

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClearChatMessagesUseCaseTest {
    private lateinit var underTest: ClearChatMessagesUseCase

    private val chatRepository = mock<ChatRepository>()
    private val chatMessageRepository = mock<ChatMessageRepository>()

    @BeforeEach
    internal fun setUp() {
        reset(
            chatRepository,
            chatMessageRepository,
        )
        underTest = ClearChatMessagesUseCase(
            chatRepository = chatRepository,
            chatMessageRepository = chatMessageRepository,
        )
    }

    @Test
    internal fun `test that messages are deleted for the selected chat id`() = runTest {
        val expectedChatId = 123L
        underTest(expectedChatId, false)

        verify(chatRepository).clearChatMessages(expectedChatId)
        verifyNoInteractions(chatMessageRepository)
    }

    @Test
    internal fun `test that pending messages are cleared if flag is set to true`() = runTest {
        val expectedChatId = 123L
        underTest(expectedChatId, true)

        verify(chatMessageRepository).clearChatPendingMessages(expectedChatId)
    }
}