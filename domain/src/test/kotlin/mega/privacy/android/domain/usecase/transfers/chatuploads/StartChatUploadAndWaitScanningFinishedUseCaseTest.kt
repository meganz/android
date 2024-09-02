package mega.privacy.android.domain.usecase.transfers.chatuploads

import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.MultiTransferEvent
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.usecase.transfers.uploads.UploadFilesUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StartChatUploadAndWaitScanningFinishedUseCaseTest {
    private lateinit var underTest: StartChatUploadAndWaitScanningFinishedUseCase

    private val uploadFilesUseCase = mock<UploadFilesUseCase>()
    private val getOrCreateMyChatsFilesFolderIdUseCase = mock<GetOrCreateMyChatsFilesFolderIdUseCase>()
    private val handleChatUploadTransferEventUseCase = mock<HandleChatUploadTransferEventUseCase>()

    @BeforeAll
    fun setup() {
        underTest = StartChatUploadAndWaitScanningFinishedUseCase(
            uploadFilesUseCase,
            getOrCreateMyChatsFilesFolderIdUseCase,
            handleChatUploadTransferEventUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() = runTest {
        reset(
            uploadFilesUseCase,
            getOrCreateMyChatsFilesFolderIdUseCase,
            handleChatUploadTransferEventUseCase,
        )
        whenever(getOrCreateMyChatsFilesFolderIdUseCase()) doReturn myChatsFolderId
    }

    @Test
    fun `test that upload files use case is invoked when the use case is invoked`() = runTest {
        val filesAndNames = mapOf(mock<File>() to "name")
        val pendingMessageIds = listOf(1L, 2L, 3L)
        whenever(uploadFilesUseCase(any(), NodeId(any()), any(), any())) doReturn emptyFlow()

        underTest(filesAndNames, pendingMessageIds)

        // Then
        verify(uploadFilesUseCase).invoke(
            filesAndNames,
            myChatsFolderId,
            pendingMessageIds.map {
                TransferAppData.ChatUpload(it)
            },
            false
        )
    }

    @Test
    fun `test that handle chat upload transfer event use case is invoked when an event is received`() =
        runTest {
            val expected = MultiTransferEvent.SingleTransferEvent(
                mock<TransferEvent.TransferStartEvent>(),
                0L,
                0L
            )
            val filesAndNames = mapOf(mock<File>() to "name")
            val pendingMessageIds = listOf(1L, 2L, 3L)
            whenever(
                uploadFilesUseCase(
                    any(),
                    NodeId(any()),
                    any(),
                    any(),
                )
            ) doReturn flowOf(expected)

            underTest(filesAndNames, pendingMessageIds)

            verify(handleChatUploadTransferEventUseCase).invoke(
                expected,
                pendingMessageIds = pendingMessageIds.toLongArray()
            )
        }

    @Test
    fun `test that the scanning finished event is awaited when the use case is invoked`() =
        runTest {
            assertDoesNotThrow {
                val finishEvent = MultiTransferEvent.SingleTransferEvent(
                    mock<TransferEvent.TransferStartEvent>(),
                    0L,
                    0L,
                    scanningFinished = true,
                )
                val filesAndNames = mapOf(mock<File>() to "name")
                val pendingMessageIds = listOf(1L, 2L, 3L)
                whenever(
                    uploadFilesUseCase(
                        any(),
                        NodeId(any()),
                        any(),
                        any(),
                    )
                ) doReturn flow {
                    emit(finishEvent)
                    throw RuntimeException()
                }

                underTest(filesAndNames, pendingMessageIds)

                verify(handleChatUploadTransferEventUseCase).invoke(
                    finishEvent,
                    pendingMessageIds = pendingMessageIds.toLongArray()
                )
            }
        }

    private val myChatsFolderId = NodeId(45L)
}
