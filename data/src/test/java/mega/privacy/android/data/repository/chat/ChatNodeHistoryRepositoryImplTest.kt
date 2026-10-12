package mega.privacy.android.data.repository.chat

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.mapper.chat.ChatHistoryLoadStatusMapper
import mega.privacy.android.data.mapper.chat.ChatMessageMapper
import mega.privacy.android.data.model.ChatNodeHistoryUpdate
import mega.privacy.android.domain.entity.chat.ChatHistoryLoadStatus
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.ChatNodeHistoryEvent
import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaChatMessage
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatNodeHistoryRepositoryImplTest {

    private lateinit var underTest: ChatNodeHistoryRepositoryImpl

    private val megaChatApiGateway = mock<MegaChatApiGateway>()
    private val chatMessageMapper = mock<ChatMessageMapper>()
    private val chatHistoryLoadStatusMapper = ChatHistoryLoadStatusMapper()

    private val chatId = 123L

    @BeforeEach
    fun setUp() {
        reset(megaChatApiGateway, chatMessageMapper)
        underTest = ChatNodeHistoryRepositoryImpl(
            megaChatApiGateway = megaChatApiGateway,
            chatMessageMapper = chatMessageMapper,
            chatHistoryLoadStatusMapper = chatHistoryLoadStatusMapper,
            ioDispatcher = UnconfinedTestDispatcher(),
        )
    }

    @Test
    fun `test that monitorNodeHistory emits AttachmentLoaded when a message is loaded`() =
        runTest {
            val megaChatMessage = mock<MegaChatMessage>()
            val chatMessage = mock<ChatMessage>()
            whenever(chatMessageMapper(megaChatMessage)).thenReturn(chatMessage)
            stubNodeHistory(ChatNodeHistoryUpdate.Loaded(megaChatMessage))

            underTest.monitorNodeHistory(chatId).test {
                assertThat(awaitItem()).isEqualTo(ChatNodeHistoryEvent.AttachmentLoaded(chatMessage))
                awaitComplete()
            }
        }

    @Test
    fun `test that monitorNodeHistory emits HistoryOpened when the node history opens`() =
        runTest {
            stubNodeHistory(ChatNodeHistoryUpdate.Opened)

            underTest.monitorNodeHistory(chatId).test {
                assertThat(awaitItem()).isEqualTo(ChatNodeHistoryEvent.HistoryOpened)
                awaitComplete()
            }
        }

    @Test
    fun `test that monitorNodeHistory emits LoadBatchFinished when the loaded message is null`() =
        runTest {
            stubNodeHistory(ChatNodeHistoryUpdate.Loaded(null))

            underTest.monitorNodeHistory(chatId).test {
                assertThat(awaitItem()).isEqualTo(ChatNodeHistoryEvent.LoadBatchFinished)
                awaitComplete()
            }
        }

    @Test
    fun `test that monitorNodeHistory emits AttachmentReceived when a message is received`() =
        runTest {
            val megaChatMessage = mock<MegaChatMessage>()
            val chatMessage = mock<ChatMessage>()
            whenever(chatMessageMapper(megaChatMessage)).thenReturn(chatMessage)
            stubNodeHistory(ChatNodeHistoryUpdate.Received(megaChatMessage))

            underTest.monitorNodeHistory(chatId).test {
                assertThat(awaitItem())
                    .isEqualTo(ChatNodeHistoryEvent.AttachmentReceived(chatMessage))
                awaitComplete()
            }
        }

    @Test
    fun `test that monitorNodeHistory emits AttachmentDeleted when a message is deleted`() =
        runTest {
            val messageId = 456L
            stubNodeHistory(ChatNodeHistoryUpdate.Deleted(messageId))

            underTest.monitorNodeHistory(chatId).test {
                assertThat(awaitItem()).isEqualTo(ChatNodeHistoryEvent.AttachmentDeleted(messageId))
                awaitComplete()
            }
        }

    @Test
    fun `test that monitorNodeHistory emits HistoryTruncated when the history is truncated`() =
        runTest {
            val messageId = 789L
            stubNodeHistory(ChatNodeHistoryUpdate.Truncated(messageId))

            underTest.monitorNodeHistory(chatId).test {
                assertThat(awaitItem()).isEqualTo(ChatNodeHistoryEvent.HistoryTruncated(messageId))
                awaitComplete()
            }
        }

    @Test
    fun `test that monitorNodeHistory emits every event in order`() = runTest {
        val first = mock<MegaChatMessage>()
        val second = mock<MegaChatMessage>()
        val firstMessage = mock<ChatMessage>()
        val secondMessage = mock<ChatMessage>()
        whenever(chatMessageMapper(first)).thenReturn(firstMessage)
        whenever(chatMessageMapper(second)).thenReturn(secondMessage)
        stubNodeHistory(
            ChatNodeHistoryUpdate.Loaded(first),
            ChatNodeHistoryUpdate.Loaded(second),
            ChatNodeHistoryUpdate.Loaded(null),
        )

        underTest.monitorNodeHistory(chatId).test {
            assertThat(awaitItem()).isEqualTo(ChatNodeHistoryEvent.AttachmentLoaded(firstMessage))
            assertThat(awaitItem()).isEqualTo(ChatNodeHistoryEvent.AttachmentLoaded(secondMessage))
            assertThat(awaitItem()).isEqualTo(ChatNodeHistoryEvent.LoadBatchFinished)
            awaitComplete()
        }
    }

    @Test
    fun `test that loadNodeHistory returns LOCAL when the gateway returns SOURCE_LOCAL`() =
        runTest {
            val count = 32
            whenever(megaChatApiGateway.loadAttachments(chatId, count))
                .thenReturn(MegaChatApi.SOURCE_LOCAL)

            assertThat(underTest.loadNodeHistory(chatId, count))
                .isEqualTo(ChatHistoryLoadStatus.LOCAL)
        }

    @Test
    fun `test that loadNodeHistory returns REMOTE when the gateway returns SOURCE_REMOTE`() =
        runTest {
            val count = 32
            whenever(megaChatApiGateway.loadAttachments(chatId, count))
                .thenReturn(MegaChatApi.SOURCE_REMOTE)

            assertThat(underTest.loadNodeHistory(chatId, count))
                .isEqualTo(ChatHistoryLoadStatus.REMOTE)
        }

    @Test
    fun `test that loadNodeHistory returns NONE when the gateway returns SOURCE_NONE`() = runTest {
        val count = 32
        whenever(megaChatApiGateway.loadAttachments(chatId, count))
            .thenReturn(MegaChatApi.SOURCE_NONE)

        assertThat(underTest.loadNodeHistory(chatId, count)).isEqualTo(ChatHistoryLoadStatus.NONE)
    }

    @Test
    fun `test that loadNodeHistory returns INVALID_CHAT when the gateway returns SOURCE_INVALID_CHAT`() =
        runTest {
            val count = 32
            whenever(megaChatApiGateway.loadAttachments(chatId, count))
                .thenReturn(MegaChatApi.SOURCE_INVALID_CHAT)

            assertThat(underTest.loadNodeHistory(chatId, count))
                .isEqualTo(ChatHistoryLoadStatus.INVALID_CHAT)
        }

    private fun stubNodeHistory(vararg updates: ChatNodeHistoryUpdate) {
        whenever(megaChatApiGateway.monitorNodeHistory(chatId)).thenReturn(flowOf(*updates))
    }
}
