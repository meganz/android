package mega.privacy.android.domain.usecase.chat.message

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.room.update.HistoryTruncatedByRetentionTime
import mega.privacy.android.domain.entity.chat.room.update.MessageUpdate
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import mega.privacy.android.domain.testutils.hotFlow
import mega.privacy.android.domain.usecase.chat.message.paging.SaveChatMessagesUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

class MonitorChatRoomMessageUpdatesUseCaseTest {
    private lateinit var underTest: MonitorChatRoomMessageUpdatesUseCase

    private val chatRepository = mock<ChatRepository>()
    private val saveChatMessagesUseCase = mock<SaveChatMessagesUseCase>()
    private val chatMessageRepository = mock<ChatMessageRepository>()

    @BeforeEach
    internal fun setUp() {
        underTest = MonitorChatRoomMessageUpdatesUseCase(
            chatRepository = chatRepository,
            saveChatMessagesUseCase = saveChatMessagesUseCase,
            chatMessageRepository = chatMessageRepository
        )
    }

    @Test
    internal fun `test that messages returned are saved`() = runTest {
        val chatId = 123L
        val message = mock<ChatMessage>()
        chatRepository.stub {
            on { monitorMessageUpdates(chatId) } doReturn hotFlow(MessageUpdate(message))
        }

        val job = launch(
            start = CoroutineStart.UNDISPATCHED
        ) { underTest(chatId) }

        verify(saveChatMessagesUseCase).invoke(chatId, listOf(message))

        job.cancelAndJoin()
    }

    @Test
    internal fun `test that messages are truncated if type is truncate`() = runTest {
        val chatId = 123L
        val truncateTime = 98765L
        val message = mock<ChatMessage> {
            on { timestamp } doReturn truncateTime
        }
        val update = HistoryTruncatedByRetentionTime(message)
        chatRepository.stub {
            on { monitorMessageUpdates(chatId) } doReturn hotFlow(update)
        }

        val job = launch(
            start = CoroutineStart.UNDISPATCHED
        ) { underTest(chatId) }

        verify(chatMessageRepository).truncateMessages(chatId, truncateTime)
        verify(saveChatMessagesUseCase, never()).invoke(chatId, listOf(message))

        job.cancelAndJoin()
    }
}