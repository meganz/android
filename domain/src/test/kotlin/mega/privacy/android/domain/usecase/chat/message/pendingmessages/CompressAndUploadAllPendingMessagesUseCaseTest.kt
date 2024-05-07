package mega.privacy.android.domain.usecase.chat.message.pendingmessages

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.Progress
import mega.privacy.android.domain.entity.chat.PendingMessage
import mega.privacy.android.domain.entity.chat.PendingMessageState
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.ChatCompressionFinished
import mega.privacy.android.domain.entity.transfer.ChatCompressionProgress
import mega.privacy.android.domain.entity.transfer.MultiTransferEvent
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.usecase.chat.ChatUploadCompressionState
import mega.privacy.android.domain.usecase.chat.ChatUploadNotCompressedReason
import mega.privacy.android.domain.usecase.chat.message.MonitorPendingMessagesByStateUseCase
import mega.privacy.android.domain.usecase.transfers.chatuploads.CompressFileForChatUseCase
import mega.privacy.android.domain.usecase.transfers.chatuploads.GetMyChatsFilesFolderIdUseCase
import mega.privacy.android.domain.usecase.transfers.chatuploads.HandleChatUploadTransferEventUseCase
import mega.privacy.android.domain.usecase.transfers.uploads.UploadFilesUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import java.io.File


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CompressAndUploadAllPendingMessagesUseCaseTest {
    private lateinit var underTest: CompressAndUploadAllPendingMessagesUseCase

    private val compressFileForChatUseCase = mock<CompressFileForChatUseCase>()
    private val uploadFilesUseCase = mock<UploadFilesUseCase>()
    private val handleChatUploadTransferEventUseCase = mock<HandleChatUploadTransferEventUseCase>()
    private val getMyChatsFilesFolderIdUseCase = mock<GetMyChatsFilesFolderIdUseCase>()
    private val monitorPendingMessagesByStateUseCase = mock<MonitorPendingMessagesByStateUseCase>()

    @BeforeAll
    fun setup() {
        underTest = CompressAndUploadAllPendingMessagesUseCase(
            compressFileForChatUseCase,
            uploadFilesUseCase,
            handleChatUploadTransferEventUseCase,
            getMyChatsFilesFolderIdUseCase,
            monitorPendingMessagesByStateUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            compressFileForChatUseCase,
            uploadFilesUseCase,
            handleChatUploadTransferEventUseCase,
            getMyChatsFilesFolderIdUseCase,
            monitorPendingMessagesByStateUseCase,
        )
        defaultStub()
    }

    private fun defaultStub() = runTest {
        whenever(getMyChatsFilesFolderIdUseCase()) doReturn NodeId(64L)
        whenever(compressFileForChatUseCase(any())) doReturn
                flowOf(ChatUploadCompressionState.NotCompressed(ChatUploadNotCompressedReason.CompressionNotNeeded))
        whenever(uploadFilesUseCase(any(), NodeId(any()), any(), any(), any())) doReturn
                flowOf(
                    MultiTransferEvent.SingleTransferEvent(
                        mock<TransferEvent.TransferStartEvent>(),
                        0,
                        0
                    )
                )
    }

    @Test
    fun `test that ChatCompressionFinished is emitted when there are no pending message in compressing state`() =
        runTest {
            whenever(monitorPendingMessagesByStateUseCase(PendingMessageState.COMPRESSING)) doReturn
                    flowOf(emptyList())
            underTest().test {
                val actual = awaitItem()
                assertThat(actual).isEqualTo(ChatCompressionFinished)
                awaitComplete()
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
                awaitComplete()
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
        whenever(compressFileForChatUseCase(any())) doReturn flowOf(
            ChatUploadCompressionState.Compressing(Progress(0f)),
            ChatUploadCompressionState.Compressing(Progress(0.5f)),
            ChatUploadCompressionState.Compressed(mock())
        )

        underTest().test {
            assertThat(awaitItem()).isEqualTo(ChatCompressionProgress(0, 1, Progress(0f)))
            assertThat(awaitItem()).isEqualTo(ChatCompressionProgress(0, 1, Progress(0.5f)))
            assertThat(awaitItem()).isEqualTo(ChatCompressionProgress(1, 1, Progress(1f)))
            awaitComplete()
        }
    }

    @Test
    fun `test that compressed file is uploaded`() = runTest {
        val pendingMessage = stubPendingMessage()
        val originalFile = File(pendingMessage.filePath)
        val compressed = File("path/compressed.mp4")
        val expected = mapOf(
            compressed to pendingMessage.name
        )
        whenever(monitorPendingMessagesByStateUseCase(PendingMessageState.COMPRESSING)) doReturn
                flowOf(listOf(pendingMessage))
        whenever(compressFileForChatUseCase(originalFile)) doReturn
                flowOf(ChatUploadCompressionState.Compressed(compressed))

        underTest().test {
            cancelAndIgnoreRemainingEvents()
        }

        verify(uploadFilesUseCase).invoke(eq(expected), NodeId(any()), any(), any(), any())
    }

    @Test
    fun `test that first upload event is handled`() = runTest {
        val pendingMessage = stubPendingMessage()
        val originalFile = File(pendingMessage.filePath)
        val compressed = File("path/compressed.mp4")
        val expected = MultiTransferEvent.SingleTransferEvent(
            mock<TransferEvent.TransferStartEvent>(),
            0,
            0
        )
        whenever(monitorPendingMessagesByStateUseCase(PendingMessageState.COMPRESSING)) doReturn
                flowOf(listOf(pendingMessage))
        whenever(compressFileForChatUseCase(originalFile)) doReturn
                flowOf(ChatUploadCompressionState.Compressed(compressed))
        whenever(uploadFilesUseCase(any(), NodeId(any()), any(), any(), any())) doReturn
                flowOf(expected)

        underTest().test {
            cancelAndIgnoreRemainingEvents()
        }

        verify(handleChatUploadTransferEventUseCase).invoke(expected, pendingMessage.id)

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