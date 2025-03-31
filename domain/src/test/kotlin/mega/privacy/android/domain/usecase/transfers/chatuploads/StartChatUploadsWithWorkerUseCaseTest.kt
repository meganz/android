package mega.privacy.android.domain.usecase.transfers.chatuploads

import app.cash.turbine.Event
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.PendingMessage
import mega.privacy.android.domain.entity.chat.PendingMessageState
import mega.privacy.android.domain.entity.chat.messages.pending.UpdatePendingMessageStateRequest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.isFileTransfer
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.exception.chat.FoldersNotAllowedAsChatUploadException
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.canceltoken.CancelCancelTokenUseCase
import mega.privacy.android.domain.usecase.chat.message.UpdatePendingMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.pendingmessages.GetPendingMessageUseCase
import mega.privacy.android.domain.usecase.transfers.uploads.UploadFileUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.AdditionalAnswers
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.anyValueClass
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

    private val uploadFileUseCase = mock<UploadFileUseCase>()
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
            uploadFileUseCase = uploadFileUseCase,
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
            uploadFileUseCase,
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
        mockFlow(emptyFlow())
    }

    @Test
    fun `test that the file is send to upload files use case`() = runTest {
        val file = mockFile()
        underTest(file, NodeId(11L), 1L).test {
            verify(uploadFileUseCase).invoke(
                uriPath = UriPath(eq(file.absolutePath)),
                fileName = anyOrNull(),
                appData = any(),
                parentFolderId = anyValueClass(),
                any(),
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that the file is send as high priority`() = runTest {
        val file = mockFile()
        underTest(file, NodeId(11L), 1L).test {
            verify(uploadFileUseCase).invoke(
                uriPath = anyValueClass(),
                fileName = anyOrNull(),
                appData = any(),
                parentFolderId = anyValueClass(),
                isHighPriority = eq(true)
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that a folder throws FoldersNotAllowedAsChatUploadException`() = runTest {
        val folder = mockFile()
        whenever(fileSystemRepository.isFilePath(folder.path)) doReturn false
        underTest(folder, NodeId(11L), 1L).test {
            val notStartedEvents = cancelAndConsumeRemainingEvents()
                .mapNotNull { (it as? Event.Error)?.throwable }
                .filterIsInstance<FoldersNotAllowedAsChatUploadException>()
            assertThat(notStartedEvents.size).isEqualTo(1)
        }
    }

    @Test
    fun `test that chatFilesFolderId is used as destination`() = runTest {
        val chatFilesFolderId = NodeId(11L)
        underTest(mockFile(), chatFilesFolderId, 1L).test {
            verify(uploadFileUseCase).invoke(
                uriPath = anyValueClass(),
                fileName = anyOrNull(),
                appData = any(),
                parentFolderId = NodeId(eq(chatFilesFolderId.longValue)),
                isHighPriority = any(),
            )
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `test that chat upload app data is set`() = runTest {
        val pendingMessageId = 1L
        val file = mockFile()
        val expected = listOf(TransferAppData.ChatUpload(pendingMessageId))
        underTest(file, NodeId(11L), pendingMessageId).test {
            verify(uploadFileUseCase).invoke(
                uriPath = anyValueClass(),
                fileName = anyOrNull(),
                appData = eq(expected),
                parentFolderId = anyValueClass(),
                isHighPriority = any(),
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that chat upload app data is set correctly when there are multiple pending messages ids`() =
        runTest {
            val pendingMessageIds = longArrayOf(1L, 2L, 3L)
            val file = mockFile()
            val expected = pendingMessageIds.map { TransferAppData.ChatUpload(it) }
            underTest(file, NodeId(11L), *pendingMessageIds).test {
                verify(uploadFileUseCase).invoke(
                    uriPath = anyValueClass(),
                    fileName = anyOrNull(),
                    appData = eq(expected),
                    parentFolderId = NodeId(any()),
                    isHighPriority = any(),
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that worker is started when start download finish correctly`() = runTest {
        mockFlow(
            flow {
                emit(mock<TransferEvent.TransferStartEvent> {
                    on { transfer } doReturn fileTransfer
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
                emit(mock<TransferEvent.TransferStartEvent> {
                    on { transfer } doReturn fileTransfer
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
            whenever(chatAttachmentNeedsCompressionUseCase(anyValueClass())) doReturn true
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
            verify(uploadFileUseCase).invoke(
                uriPath = anyValueClass(),
                fileName = eq(pendingMessageName),
                appData = anyOrNull(),
                parentFolderId = anyValueClass(),
                isHighPriority = eq(true),
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
            val event = TransferEvent.TransferStartEvent(transfer)
            whenever(
                uploadFileUseCase.invoke(
                    uriPath = anyValueClass(),
                    fileName = anyOrNull(),
                    appData = any(),
                    parentFolderId = anyValueClass(),
                    isHighPriority = any()
                )
            ) doReturn flowOf(event)

            underTest(file, NodeId(11L), pendingMessageId).test {
                verify(handleChatUploadTransferEventUseCase).invoke(event, pendingMessageId)
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun mockFile() = mock<File> {
        on { isDirectory }.thenReturn(false)
        on { path }.thenReturn("path")
        on { absolutePath }.thenReturn("path")
    }

    private val fileTransfer = mock<Transfer> {
        on { isFolderTransfer } doReturn false
    }

    private fun mockFlow(flow: Flow<TransferEvent>) {
        whenever(
            uploadFileUseCase.invoke(
                uriPath = anyValueClass(),
                fileName = anyOrNull(),
                appData = anyOrNull(),
                parentFolderId = anyValueClass(),
                isHighPriority = any(),
            )
        ).thenReturn(flow)
    }
}