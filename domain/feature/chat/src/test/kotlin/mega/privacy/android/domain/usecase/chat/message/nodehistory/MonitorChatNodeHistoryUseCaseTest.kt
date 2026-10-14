package mega.privacy.android.domain.usecase.chat.message.nodehistory

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.ChatNodeHistoryEvent
import mega.privacy.android.domain.entity.chat.ChatNodeHistoryFileEvent
import mega.privacy.android.domain.entity.chat.messages.NodeAttachmentMessage
import mega.privacy.android.domain.repository.chat.ChatNodeHistoryRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorChatNodeHistoryUseCaseTest {
    private lateinit var underTest: MonitorChatNodeHistoryUseCase

    private val chatNodeHistoryRepository = mock<ChatNodeHistoryRepository>()
    private val createNodeHistoryAttachmentMessageUseCase =
        mock<CreateNodeHistoryAttachmentMessageUseCase>()

    private val chatId = 123L

    @BeforeAll
    fun setUp() {
        underTest = MonitorChatNodeHistoryUseCase(
            chatNodeHistoryRepository = chatNodeHistoryRepository,
            createNodeHistoryAttachmentMessageUseCase = createNodeHistoryAttachmentMessageUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(chatNodeHistoryRepository, createNodeHistoryAttachmentMessageUseCase)
    }

    @Test
    fun `test that invoke emits the history events of the chat from the repository`() =
        runTest {
            stubEvents(
                ChatNodeHistoryEvent.HistoryOpened,
                ChatNodeHistoryEvent.AttachmentDeleted(1L),
                ChatNodeHistoryEvent.HistoryTruncated(2L),
            )

            underTest(chatId).test {
                assertThat(awaitItem()).isEqualTo(ChatNodeHistoryFileEvent.HistoryOpened)
                assertThat(awaitItem()).isEqualTo(ChatNodeHistoryFileEvent.FileDeleted(1L))
                assertThat(awaitItem()).isEqualTo(ChatNodeHistoryFileEvent.HistoryTruncated(2L))
                awaitComplete()
            }
        }

    @Test
    fun `test that invoke emits the node attachment message when an attachment is received`() =
        runTest {
            val message = mock<ChatMessage>()
            val attachment = mock<NodeAttachmentMessage>()
            whenever(createNodeHistoryAttachmentMessageUseCase(chatId, message))
                .thenReturn(attachment)
            stubEvents(ChatNodeHistoryEvent.AttachmentReceived(message))

            underTest(chatId).test {
                assertThat(awaitItem()).isEqualTo(ChatNodeHistoryFileEvent.FileReceived(attachment))
                awaitComplete()
            }
        }

    @Test
    fun `test that invoke skips a received attachment when it has no file`() = runTest {
        val message = mock<ChatMessage>()
        whenever(createNodeHistoryAttachmentMessageUseCase(chatId, message)).thenReturn(null)
        stubEvents(
            ChatNodeHistoryEvent.AttachmentReceived(message),
            ChatNodeHistoryEvent.AttachmentDeleted(1L),
        )

        underTest(chatId).test {
            assertThat(awaitItem()).isEqualTo(ChatNodeHistoryFileEvent.FileDeleted(1L))
            awaitComplete()
        }
    }

    @Test
    fun `test that invoke does not emit the loaded attachments of a page`() = runTest {
        stubEvents(
            ChatNodeHistoryEvent.AttachmentLoaded(mock()),
            ChatNodeHistoryEvent.LoadBatchFinished,
        )

        underTest(chatId).test {
            awaitComplete()
        }
        verify(createNodeHistoryAttachmentMessageUseCase, never()).invoke(any(), any())
    }

    private fun stubEvents(vararg events: ChatNodeHistoryEvent) {
        whenever(chatNodeHistoryRepository.monitorNodeHistory(chatId))
            .thenReturn(flowOf(*events))
    }
}
