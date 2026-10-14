package mega.privacy.android.domain.usecase.chat.message.nodehistory

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatHistoryLoadStatus
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.ChatNodeHistoryEvent
import mega.privacy.android.domain.entity.chat.ChatNodeHistoryPage
import mega.privacy.android.domain.entity.chat.messages.NodeAttachmentMessage
import mega.privacy.android.domain.exception.ChatRoomDoesNotExistException
import mega.privacy.android.domain.exception.chat.ChatNodeHistoryLoadException
import mega.privacy.android.domain.repository.chat.ChatNodeHistoryRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LoadChatNodeHistoryPageUseCaseTest {
    private lateinit var underTest: LoadChatNodeHistoryPageUseCase

    private val chatNodeHistoryRepository = mock<ChatNodeHistoryRepository>()
    private val createNodeHistoryAttachmentMessageUseCase =
        mock<CreateNodeHistoryAttachmentMessageUseCase>()

    private val chatId = 123L
    private val attachments = mutableMapOf<ChatMessage, NodeAttachmentMessage>()

    private class Batch(
        val events: List<ChatNodeHistoryEvent>,
        val status: ChatHistoryLoadStatus,
    )

    @BeforeAll
    fun setUp() {
        underTest = LoadChatNodeHistoryPageUseCase(
            chatNodeHistoryRepository = chatNodeHistoryRepository,
            createNodeHistoryAttachmentMessageUseCase = createNodeHistoryAttachmentMessageUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(chatNodeHistoryRepository, createNodeHistoryAttachmentMessageUseCase)
        attachments.clear()
        createNodeHistoryAttachmentMessageUseCase.stub {
            onBlocking { invoke(any(), any()) } doSuspendableAnswer {
                attachmentOf(it.getArgument(1))
            }
        }
    }

    @Test
    fun `test that invoke does not load the history before it is opened`() = runTest {
        val events = stubNodeHistory(
            batch(ChatHistoryLoadStatus.NONE),
            initialEvents = emptyList(),
        )

        val page = async { underTest(chatId = chatId, pageSize = 10) }
        runCurrent()

        verify(chatNodeHistoryRepository, never()).loadNodeHistory(any(), any())

        events.send(ChatNodeHistoryEvent.HistoryOpened)
        page.await()

        verify(chatNodeHistoryRepository).loadNodeHistory(chatId, 10)
    }

    @Test
    fun `test that invoke returns the loaded messages when a single batch fills the page`() =
        runTest {
            val messages = listOf(mock<ChatMessage>(), mock<ChatMessage>())
            stubNodeHistory(batch(ChatHistoryLoadStatus.LOCAL, *messages.toTypedArray()))

            val actual = underTest(chatId = chatId, pageSize = 2)

            assertThat(actual).isEqualTo(
                ChatNodeHistoryPage(messages = messages.map(::attachmentOf), hasMore = true)
            )
            verify(chatNodeHistoryRepository, times(1)).loadNodeHistory(any(), any())
        }

    @Test
    fun `test that invoke loads batches until the page is full when a batch returns fewer messages`() =
        runTest {
            val first = mock<ChatMessage>()
            val second = mock<ChatMessage>()
            val third = mock<ChatMessage>()
            stubNodeHistory(
                batch(ChatHistoryLoadStatus.REMOTE, first),
                batch(ChatHistoryLoadStatus.LOCAL, second, third),
            )

            val actual = underTest(chatId = chatId, pageSize = 3)

            assertThat(actual).isEqualTo(
                ChatNodeHistoryPage(
                    messages = listOf(first, second, third).map(::attachmentOf),
                    hasMore = true,
                )
            )
            inOrder(chatNodeHistoryRepository) {
                verify(chatNodeHistoryRepository).loadNodeHistory(chatId, 3)
                verify(chatNodeHistoryRepository).loadNodeHistory(chatId, 2)
            }
        }

    @Test
    fun `test that invoke returns no more history when the status is NONE`() = runTest {
        val message = mock<ChatMessage>()
        stubNodeHistory(batch(ChatHistoryLoadStatus.NONE, message))

        val actual = underTest(chatId = chatId, pageSize = 5)

        assertThat(actual).isEqualTo(
            ChatNodeHistoryPage(messages = listOf(attachmentOf(message)), hasMore = false)
        )
        verify(chatNodeHistoryRepository, times(1)).loadNodeHistory(any(), any())
    }

    @Test
    fun `test that invoke returns an empty page when the history is empty`() = runTest {
        stubNodeHistory(batch(ChatHistoryLoadStatus.NONE))

        val actual = underTest(chatId = chatId, pageSize = 5)

        assertThat(actual).isEqualTo(ChatNodeHistoryPage(messages = emptyList(), hasMore = false))
    }

    @Test
    fun `test that invoke loads only once when the history is opened more than once`() =
        runTest {
            val message = mock<ChatMessage>()
            stubNodeHistory(
                batch(ChatHistoryLoadStatus.LOCAL, message),
                initialEvents = listOf(
                    ChatNodeHistoryEvent.HistoryOpened,
                    ChatNodeHistoryEvent.HistoryOpened,
                ),
            )

            val actual = underTest(chatId = chatId, pageSize = 1)

            assertThat(actual.messages).containsExactly(attachmentOf(message))
            verify(chatNodeHistoryRepository, times(1)).loadNodeHistory(any(), any())
        }

    @Test
    fun `test that invoke ignores live attachment events when they arrive during a batch`() =
        runTest {
            val loaded = mock<ChatMessage>()
            stubNodeHistory(
                Batch(
                    events = listOf(
                        ChatNodeHistoryEvent.AttachmentReceived(mock()),
                        ChatNodeHistoryEvent.AttachmentLoaded(loaded),
                        ChatNodeHistoryEvent.AttachmentDeleted(1L),
                        ChatNodeHistoryEvent.HistoryTruncated(2L),
                        ChatNodeHistoryEvent.LoadBatchFinished,
                    ),
                    status = ChatHistoryLoadStatus.NONE,
                )
            )

            val actual = underTest(chatId = chatId, pageSize = 5)

            assertThat(actual.messages).containsExactly(attachmentOf(loaded))
        }

    @Test
    fun `test that invoke ignores loaded attachments when they arrive before the history is opened`() =
        runTest {
            val own = mock<ChatMessage>()
            stubNodeHistory(
                batch(ChatHistoryLoadStatus.LOCAL, own),
                initialEvents = listOf(
                    ChatNodeHistoryEvent.AttachmentLoaded(mock()),
                    ChatNodeHistoryEvent.LoadBatchFinished,
                    ChatNodeHistoryEvent.HistoryOpened,
                ),
            )

            val actual = underTest(chatId = chatId, pageSize = 1)

            assertThat(actual.messages).containsExactly(attachmentOf(own))
        }

    @Test
    fun `test that invoke throws ChatNodeHistoryLoadException when the status is ERROR`() =
        runTest {
            stubNodeHistory(Batch(events = emptyList(), status = ChatHistoryLoadStatus.ERROR))

            val actual = runCatching { underTest(chatId = chatId, pageSize = 5) }

            assertThat(actual.exceptionOrNull())
                .isInstanceOf(ChatNodeHistoryLoadException::class.java)
        }

    @Test
    fun `test that invoke throws ChatRoomDoesNotExistException when the status is INVALID_CHAT`() =
        runTest {
            stubNodeHistory(
                Batch(events = emptyList(), status = ChatHistoryLoadStatus.INVALID_CHAT)
            )

            val actual = runCatching { underTest(chatId = chatId, pageSize = 5) }

            assertThat(actual.exceptionOrNull())
                .isInstanceOf(ChatRoomDoesNotExistException::class.java)
        }

    @Test
    fun `test that invoke throws when the history cannot be opened`() = runTest {
        whenever(chatNodeHistoryRepository.monitorNodeHistory(chatId)).thenReturn(
            flow { throw ChatRoomDoesNotExistException() }
        )

        val actual = runCatching { underTest(chatId = chatId, pageSize = 5) }

        assertThat(actual.exceptionOrNull())
            .isInstanceOf(ChatRoomDoesNotExistException::class.java)
        verify(chatNodeHistoryRepository, never()).loadNodeHistory(any(), any())
    }

    @Test
    fun `test that invoke loads more when messages without a file leave the page short`() =
        runTest {
            val withoutFile = mock<ChatMessage>()
            val first = mock<ChatMessage>()
            val second = mock<ChatMessage>()
            createNodeHistoryAttachmentMessageUseCase.stub {
                onBlocking { invoke(chatId, withoutFile) } doReturn null
            }
            stubNodeHistory(
                batch(ChatHistoryLoadStatus.REMOTE, first, withoutFile),
                batch(ChatHistoryLoadStatus.LOCAL, second),
            )

            val actual = underTest(chatId = chatId, pageSize = 2)

            assertThat(actual.messages)
                .containsExactly(attachmentOf(first), attachmentOf(second))
                .inOrder()
            inOrder(chatNodeHistoryRepository) {
                verify(chatNodeHistoryRepository).loadNodeHistory(chatId, 2)
                verify(chatNodeHistoryRepository).loadNodeHistory(chatId, 1)
            }
        }

    @Test
    fun `test that invoke stops loading when a batch loads no attachment`() = runTest {
        stubNodeHistory(batch(ChatHistoryLoadStatus.REMOTE))

        val actual = underTest(chatId = chatId, pageSize = 5)

        assertThat(actual).isEqualTo(ChatNodeHistoryPage(messages = emptyList(), hasMore = true))
        verify(chatNodeHistoryRepository, times(1)).loadNodeHistory(any(), any())
    }

    @Test
    fun `test that invoke loads more when a batch loads only messages without a file`() =
        runTest {
            val withoutFile = mock<ChatMessage>()
            val message = mock<ChatMessage>()
            createNodeHistoryAttachmentMessageUseCase.stub {
                onBlocking { invoke(chatId, withoutFile) } doReturn null
            }
            stubNodeHistory(
                batch(ChatHistoryLoadStatus.REMOTE, withoutFile),
                batch(ChatHistoryLoadStatus.NONE, message),
            )

            val actual = underTest(chatId = chatId, pageSize = 1)

            assertThat(actual).isEqualTo(
                ChatNodeHistoryPage(messages = listOf(attachmentOf(message)), hasMore = false)
            )
            verify(chatNodeHistoryRepository, times(2)).loadNodeHistory(chatId, 1)
        }

    private fun attachmentOf(message: ChatMessage): NodeAttachmentMessage =
        attachments.getOrPut(message) { mock() }

    private fun batch(status: ChatHistoryLoadStatus, vararg messages: ChatMessage) = Batch(
        events = messages.map { ChatNodeHistoryEvent.AttachmentLoaded(it) } +
                ChatNodeHistoryEvent.LoadBatchFinished,
        status = status,
    )

    private fun stubNodeHistory(
        vararg batches: Batch,
        initialEvents: List<ChatNodeHistoryEvent> = listOf(ChatNodeHistoryEvent.HistoryOpened),
    ): Channel<ChatNodeHistoryEvent> {
        val events = Channel<ChatNodeHistoryEvent>(Channel.UNLIMITED)
        initialEvents.forEach { events.trySend(it) }
        val pending = ArrayDeque(batches.toList())
        whenever(chatNodeHistoryRepository.monitorNodeHistory(chatId))
            .thenReturn(events.receiveAsFlow())
        chatNodeHistoryRepository.stub {
            on { loadNodeHistory(any(), any()) } doSuspendableAnswer {
                val next = pending.removeFirst()
                next.events.forEach { events.send(it) }
                next.status
            }
        }
        return events
    }
}
