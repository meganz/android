package mega.privacy.android.domain.usecase.chat.message

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.testutils.hotFlow
import mega.privacy.android.domain.usecase.chat.message.paging.SaveChatMessagesUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

class MonitorChatRoomMessageUpdatesUseCaseTest {
    private lateinit var underTest: MonitorChatRoomMessageUpdatesUseCase

    private val chatRepository = mock<ChatRepository>()
    private val saveChatMessagesUseCase = mock<SaveChatMessagesUseCase>()

    @BeforeEach
    internal fun setUp() {
        underTest = MonitorChatRoomMessageUpdatesUseCase(
            chatRepository = chatRepository,
            saveChatMessagesUseCase = saveChatMessagesUseCase,
        )
    }

    @Test
    internal fun `test that messages returned are saved`() = runTest {
        val chatId = 123L
        val message = mock<ChatMessage>()
        chatRepository.stub {
            on { monitorMessageUpdates(chatId) } doReturn hotFlow(message)
        }

        val job = launch(
            start = CoroutineStart.UNDISPATCHED
        ) { underTest(chatId) }

        verify(saveChatMessagesUseCase).invoke(chatId, listOf(message))

        job.cancelAndJoin()
    }
}