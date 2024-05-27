package mega.privacy.android.domain.usecase.chat.message.pendingmessages

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.chat.PendingMessage
import mega.privacy.android.domain.entity.chat.PendingMessageState
import mega.privacy.android.domain.entity.chat.messages.pending.UpdatePendingMessageStateAndPathRequest
import mega.privacy.android.domain.entity.transfer.ChatCompressionFinished
import mega.privacy.android.domain.entity.transfer.ChatCompressionProgress
import mega.privacy.android.domain.repository.chat.ChatMessageRepository
import mega.privacy.android.domain.usecase.chat.ChatUploadCompressionState
import mega.privacy.android.domain.usecase.chat.ChatUploadNotCompressedReason
import mega.privacy.android.domain.usecase.chat.message.MonitorPendingMessagesByStateUseCase
import mega.privacy.android.domain.usecase.chat.message.UpdatePendingMessageUseCase
import mega.privacy.android.domain.usecase.transfers.chatuploads.CompressFileForChatUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import java.io.File


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CompressPendingMessagesUseCaseTest {
    private lateinit var underTest: CompressPendingMessagesUseCase

    private val compressFileForChatUseCase = mock<CompressFileForChatUseCase>()
    private val monitorPendingMessagesByStateUseCase = mock<MonitorPendingMessagesByStateUseCase>()
    private val chatMessageRepository = mock<ChatMessageRepository>()
    private val updatePendingMessageUseCase = mock<UpdatePendingMessageUseCase>()

    @BeforeAll
    fun setup() {
        underTest = CompressPendingMessagesUseCase(
            compressFileForChatUseCase,
            monitorPendingMessagesByStateUseCase,
            chatMessageRepository,
            updatePendingMessageUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            compressFileForChatUseCase,
            monitorPendingMessagesByStateUseCase,
            chatMessageRepository,
            updatePendingMessageUseCase,
        )
        defaultStub()
    }

    private fun defaultStub() = runTest {
        whenever(compressFileForChatUseCase(any())) doReturn
                flowOf(ChatUploadCompressionState.NotCompressed(ChatUploadNotCompressedReason.CompressionNotNeeded))
    }

    @Test
    fun `test that ChatCompressionFinished is emitted when there are no pending message in compressing state`() =
        runTest {
            whenever(monitorPendingMessagesByStateUseCase(PendingMessageState.COMPRESSING)) doReturn
                    flowOf(emptyList())
            underTest().test {
                val actual = awaitItem()
                assertThat(actual).isEqualTo(ChatCompressionFinished)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that ChatCompressionFinished is emitted when there are no more pending message in compressing state`() =
        runTest {
            val firstNotEmpty = listOf(stubPendingMessage())
            whenever(monitorPendingMessagesByStateUseCase(PendingMessageState.COMPRESSING)) doReturn
                    flowOf(firstNotEmpty, emptyList())
            underTest().test {
                assertThat(awaitItem()).isEqualTo(ChatCompressionProgress(1, 1, Progress(1f)))
                assertThat(awaitItem()).isEqualTo(ChatCompressionFinished)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that compression starts for a single pending message`() =
        runTest {
            val pendingMessage = stubPendingMessage()
            val originalFile = File(pendingMessage.filePath)
            whenever(monitorPendingMessagesByStateUseCase(PendingMessageState.COMPRESSING)) doReturn
                    flowOf(listOf(pendingMessage))

            underTest().test {
                cancelAndIgnoreRemainingEvents()
            }

            verify(compressFileForChatUseCase).invoke(originalFile)
        }

    @Test
    fun `test that compression starts for multiple pending messages`() =
        runTest {
            val pendingMessage1 = stubPendingMessage(1L)
            val pendingMessage2 = stubPendingMessage(2L)
            val originalFile1 = File(pendingMessage1.filePath)
            val originalFile2 = File(pendingMessage2.filePath)
            whenever(monitorPendingMessagesByStateUseCase(PendingMessageState.COMPRESSING)) doReturn
                    flowOf(listOf(pendingMessage1, pendingMessage2))

            underTest().test {
                cancelAndIgnoreRemainingEvents()
            }

            verify(compressFileForChatUseCase).invoke(originalFile1)
            verify(compressFileForChatUseCase).invoke(originalFile2)
        }

    @Test
    fun `test that multiple pending messages with the same path are compressed only once`() =
        runTest {
            val pendingMessage1 = stubPendingMessage(1L, "name.mp4")
            val pendingMessage2 = stubPendingMessage(2L, "name.mp4")
            val originalFile = File(pendingMessage1.filePath)
            whenever(monitorPendingMessagesByStateUseCase(PendingMessageState.COMPRESSING)) doReturn
                    flowOf(listOf(pendingMessage1, pendingMessage2))


            underTest().test {
                cancelAndIgnoreRemainingEvents()
            }

            verify(compressFileForChatUseCase).invoke(originalFile)
            verifyNoMoreInteractions(compressFileForChatUseCase)
        }

    @Test
    fun `test that compression progress is emitted`() = runTest {
        val pendingMessage = stubPendingMessage()
        whenever(monitorPendingMessagesByStateUseCase(PendingMessageState.COMPRESSING)) doReturn
                flowOf(listOf(pendingMessage))
        val expectedProgress = listOf(Progress(0f), Progress(0.5f), Progress(0.9f))
        whenever(compressFileForChatUseCase(any())) doReturn
                expectedProgress.map {
                    ChatUploadCompressionState.Compressing(it)
                }
                    .plus(ChatUploadCompressionState.Compressed(mock()))
                    .asFlow()

        underTest().test {
            expectedProgress.forEach {
                assertThat(awaitItem()).isEqualTo(ChatCompressionProgress(0, 1, it))
            }
            // ChatUploadCompressionState.Compressed should be full progress
            assertThat(awaitItem()).isEqualTo(ChatCompressionProgress(1, 1, Progress(1f)))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that compression progress is send to chat message repository`() = runTest {
        val pendingMessages = listOf(stubPendingMessage())
        whenever(monitorPendingMessagesByStateUseCase(PendingMessageState.COMPRESSING)) doReturn
                flowOf(pendingMessages)
        val expectedProgress = listOf(Progress(0f), Progress(0.5f), Progress(0.9f))
        whenever(compressFileForChatUseCase(any())) doReturn
                expectedProgress.map {
                    ChatUploadCompressionState.Compressing(it)
                }
                    .plus(ChatUploadCompressionState.Compressed(mock()))
                    .asFlow()


        underTest().test {
            cancelAndIgnoreRemainingEvents()
            expectedProgress.forEach {
                verify(chatMessageRepository).updatePendingMessagesCompressionProgress(
                    it,
                    pendingMessages,
                )
            }
            // ChatUploadCompressionState.Compressed should be full progress
            verify(chatMessageRepository).updatePendingMessagesCompressionProgress(
                Progress(1f),
                pendingMessages,
            )
        }
    }

    @Test
    fun `test that compressed file is set to ready to upload state`() = runTest {
        val pendingMessage = stubPendingMessage()
        val originalFile = File(pendingMessage.filePath)
        val compressed = File("path/compressed.mp4")

        whenever(monitorPendingMessagesByStateUseCase(PendingMessageState.COMPRESSING)) doReturn
                flowOf(listOf(pendingMessage))
        whenever(compressFileForChatUseCase(originalFile)) doReturn
                flowOf(ChatUploadCompressionState.Compressed(compressed))

        underTest().test {
            cancelAndIgnoreRemainingEvents()
        }

        verify(updatePendingMessageUseCase)(
            UpdatePendingMessageStateAndPathRequest(
                pendingMessage.id,
                PendingMessageState.READY_TO_UPLOAD,
                compressed.path,
            )
        )
    }

    private fun stubPendingMessage(
        id: Long = 12L,
        fileName: String = "pendingMessage$id.mp4",
    ) = mock<PendingMessage> {
        on { this.id } doReturn id
        on { this.name } doReturn "pendingMessage$id name"
        on { this.filePath } doReturn "/path/$fileName"
    }

}