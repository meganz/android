package mega.privacy.android.domain.usecase.transfers.chatuploads

import app.cash.turbine.Event
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.PendingMessage
import mega.privacy.android.domain.entity.chat.PendingMessageState
import mega.privacy.android.domain.entity.chat.messages.pending.UpdatePendingMessageStateRequest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.MultiTransferEvent
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.canceltoken.CancelCancelTokenUseCase
import mega.privacy.android.domain.usecase.chat.message.UpdatePendingMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.pendingmessages.GetPendingMessageUseCase
import mega.privacy.android.domain.usecase.transfers.uploads.UploadFilesUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.AdditionalAnswers
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StartChatUploadsWithWorkerUseCaseTest {
    private lateinit var underTest: StartChatUploadsWithWorkerUseCase

    private val uploadFilesUseCase = mock<UploadFilesUseCase>()
    private val cancelCancelTokenUseCase = mock<CancelCancelTokenUseCase>()
    private val startChatUploadsWorkerAndWaitUntilIsStartedUseCase =
        mock<StartChatUploadsWorkerAndWaitUntilIsStartedUseCase>()
    private val fileSystemRepository = mock<FileSystemRepository>()
    private val handleChatUploadTransferEventUseCase = mock<HandleChatUploadTransferEventUseCase>()
    private val chatAttachmentNeedsCompressionUseCase =
        mock<ChatAttachmentNeedsCompressionUseCase>()
    private val updatePendingMessageUseCase = mock<UpdatePendingMessageUseCase>()
    private val getPendingMessageUseCase = mock<GetPendingMessageUseCase>()

    @BeforeAll
    fun setup() {
        underTest = StartChatUploadsWithWorkerUseCase(
            uploadFilesUseCase = uploadFilesUseCase,
            startChatUploadsWorkerAndWaitUntilIsStartedUseCase = startChatUploadsWorkerAndWaitUntilIsStartedUseCase,
            chatAttachmentNeedsCompressionUseCase = chatAttachmentNeedsCompressionUseCase,
            fileSystemRepository = fileSystemRepository,
            handleChatUploadTransferEventUseCase = handleChatUploadTransferEventUseCase,
            updatePendingMessageUseCase = updatePendingMessageUseCase,
            getPendingMessageUseCase = getPendingMessageUseCase,
            cancelCancelTokenUseCase = cancelCancelTokenUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() = runTest {
        reset(
            uploadFilesUseCase,
            startChatUploadsWorkerAndWaitUntilIsStartedUseCase,
            chatAttachmentNeedsCompressionUseCase,
            fileSystemRepository,
            handleChatUploadTransferEventUseCase,
            updatePendingMessageUseCase,
            getPendingMessageUseCase,
            cancelCancelTokenUseCase,
        )
        commonStub()
    }

    private suspend fun commonStub() {
        whenever(fileSystemRepository.isFilePath(any())) doReturn true
        whenever(chatAttachmentNeedsCompressionUseCase(any())) doReturn false
    }

    @Test
    fun `test that the file is send to upload files use case`() = runTest {
        val file = mockFile()
        underTest(file, NodeId(11L), 1L).test {
            verify(uploadFilesUseCase).invoke(
                eq(mapOf(file to null)), NodeId(any()), any(), any()
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that the file is send as high priority`() = runTest {
        val file = mockFile()
        underTest(file, NodeId(11L), 1L).test {
            verify(uploadFilesUseCase).invoke(
                filesAndNames = any(),
                parentFolderId = NodeId(any()),
                appData = any(),
                isHighPriority = eq(true)
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that a folder emits TransferNotStarted event`() = runTest {
        val folder = mockFile()
        whenever(fileSystemRepository.isFilePath(any())) doReturn false
        underTest(folder, NodeId(11L), 1L).test {
            val notStartedEvents = cancelAndConsumeRemainingEvents()
                .filterIsInstance<Event.Item<MultiTransferEvent>>()
                .map { it.value }
                .filterIsInstance<MultiTransferEvent.TransferNotStarted<*>>()
            assertThat(notStartedEvents.size).isEqualTo(1)
        }
    }

    @Test
    fun `test that chatFilesFolderId is used as destination`() = runTest {
        val chatFilesFolderId = NodeId(11L)
        underTest(mockFile(), chatFilesFolderId, 1L).test {
            verify(uploadFilesUseCase).invoke(
                any(),
                NodeId(eq(chatFilesFolderId.longValue)),
                any(),
                any(),
            )
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `test that chat upload app data is set`() = runTest {
        val pendingMessageId = 1L
        underTest(mockFile(), NodeId(11L), pendingMessageId).test {
            verify(uploadFilesUseCase).invoke(
                any(),
                NodeId(any()),
                eq(listOf(TransferAppData.ChatUpload(pendingMessageId))),
                any(),
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that chat upload app data is set correctly when there are multiple pending messages ids`() =
        runTest {
            val pendingMessageIds = longArrayOf(1L, 2L, 3L)
            underTest(mockFile(), NodeId(11L), *pendingMessageIds).test {
                verify(uploadFilesUseCase).invoke(
                    any(),
                    NodeId(any()),
                    eq(pendingMessageIds.map { TransferAppData.ChatUpload(it) }),
                    any(),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that worker is started when start download finish correctly`() = runTest {
        mockFlow(
            flow {
                emit(mock<MultiTransferEvent.SingleTransferEvent> {
                    on { scanningFinished } doReturn true
                })
                awaitCancellation()
            }
        )
        underTest(mockFile(), NodeId(11L), 1L).collect()
        verify(startChatUploadsWorkerAndWaitUntilIsStartedUseCase).invoke()
    }

    @Test
    fun `test that flow is not finished until the worker is started`() = runTest {
        var workerStarted = false
        mockFlow(
            flow {
                emit(mock<MultiTransferEvent.SingleTransferEvent> {
                    on { scanningFinished } doReturn true
                })
                awaitCancellation()
            }
        )
        whenever(startChatUploadsWorkerAndWaitUntilIsStartedUseCase()).then(
            AdditionalAnswers.answersWithDelay(
                10
            ) {
                workerStarted = true
            })
        underTest(mockFile(), NodeId(11L), 1L).test {
            awaitItem()
            awaitComplete()
            assertThat(workerStarted).isTrue()
        }
        verify(startChatUploadsWorkerAndWaitUntilIsStartedUseCase).invoke()
    }

    @Test
    fun `test that files returned by CompressFileForChatUseCase are send to upload files use case`() =
        runTest {
            val file = mockFile()
            val pendingMessageId = 1L
            whenever(chatAttachmentNeedsCompressionUseCase(file)) doReturn true
            underTest(file, NodeId(11L), pendingMessageId).test {
                verify(updatePendingMessageUseCase)
                    .invoke(
                        UpdatePendingMessageStateRequest(
                            pendingMessageId,
                            PendingMessageState.COMPRESSING
                        )
                    )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that pending message name is used when is not null`() = runTest {
        val file = mockFile()
        val pendingMessageId = 1L
        val pendingMessageName = "Rename"
        val pendingMessage = mock<PendingMessage> {
            on { name } doReturn pendingMessageName
        }
        whenever(getPendingMessageUseCase(1L)) doReturn pendingMessage
        underTest(file, NodeId(11L), pendingMessageId).test {
            verify(uploadFilesUseCase).invoke(
                eq(mapOf(file to pendingMessageName)), NodeId(any()), any(), any()
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that handle chat upload transfer event use case is called on each transfer event`() =
        runTest {
            val file = mockFile()
            val pendingMessageId = 15L
            val transferTag = 12
            val transfer = mock<Transfer> {
                on { it.tag } doReturn transferTag
            }
            val event = MultiTransferEvent.SingleTransferEvent(
                TransferEvent.TransferStartEvent(transfer), 0, 0
            )
            whenever(
                uploadFilesUseCase(any(), NodeId(any()), any(), any())
            ) doReturn flowOf(event)

            underTest(file, NodeId(11L), pendingMessageId).test {
                verify(handleChatUploadTransferEventUseCase).invoke(event, pendingMessageId)
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun mockFile() = mock<File> {
        on { isDirectory }.thenReturn(false)
        on { path }.thenReturn("path")
    }

    private fun mockFlow(flow: Flow<MultiTransferEvent>) {
        whenever(uploadFilesUseCase(any(), NodeId(any()), anyOrNull(), any()))
            .thenReturn(flow)
    }
}