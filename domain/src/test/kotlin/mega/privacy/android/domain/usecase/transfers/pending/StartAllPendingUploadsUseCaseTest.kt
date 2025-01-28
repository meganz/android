package mega.privacy.android.domain.usecase.transfers.pending

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import mega.privacy.android.domain.entity.node.DefaultTypedFileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferStage
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.isAlreadyTransferredEvent
import mega.privacy.android.domain.entity.transfer.isTransferUpdated
import mega.privacy.android.domain.entity.transfer.pending.PendingTransfer
import mega.privacy.android.domain.entity.transfer.pending.PendingTransferNodeIdentifier
import mega.privacy.android.domain.entity.transfer.pending.PendingTransferState
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.transfers.uploads.UploadFileUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.anyValueClass
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.RuntimeException

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StartAllPendingUploadsUseCaseTest {
    private lateinit var underTest: StartAllPendingUploadsUseCase

    private val transferRepository = mock<TransferRepository>()
    private val getPendingTransfersByTypeAndStateUseCase =
        mock<GetPendingTransfersByTypeAndStateUseCase>()
    private val updatePendingTransferStateUseCase = mock<UpdatePendingTransferStateUseCase>()

    private val updatePendingTransferStartedCountUseCase =
        mock<UpdatePendingTransferStartedCountUseCase>()
    private val uploadFileUseCase = mock<UploadFileUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = StartAllPendingUploadsUseCase(
            transferRepository,
            getPendingTransfersByTypeAndStateUseCase,
            updatePendingTransferStateUseCase,
            updatePendingTransferStartedCountUseCase,
            uploadFileUseCase,
        )
    }

    @BeforeEach
    fun cleanUp() {
        reset(
            transferRepository,
            getPendingTransfersByTypeAndStateUseCase,
            updatePendingTransferStateUseCase,
            updatePendingTransferStartedCountUseCase,
            uploadFileUseCase,
        )
    }

    @Test
    fun `test that the flow emits 0 when there are no pending messages in NotSentToSdk state`() =
        runTest {
            stubNotSentPendingTransfers(emptyList())

            underTest().test {
                assertThat(awaitItem()).isEqualTo(0)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that the flow emits the total amount of pending messages in NotSentToSdk state`() =
        runTest {
            stubNotSentPendingTransfers(listOf(mock(), mock()))

            underTest().test {
                assertThat(awaitItem()).isEqualTo(2)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that the state of pending transfers is updated to SdkScanning when started`() =
        runTest {
            val pendingTransfers = listOf(mock<PendingTransfer>())
            stubNotSentPendingTransfers(pendingTransfers)

            underTest().test {
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }

            verify(updatePendingTransferStateUseCase)
                .invoke(pendingTransfers, PendingTransferState.SdkScanning)
        }

    @Test
    fun `test that the flow conflates the collected pending transfers`() =
        runTest {
            val firstEmission = listOf<PendingTransfer>(mock())
            val secondEmission = listOf<PendingTransfer>(mock(), mock())
            val thirdEmission = listOf<PendingTransfer>(mock(), mock(), mock())
            stubNotSentPendingTransfers(firstEmission, secondEmission, thirdEmission)
            whenever(
                updatePendingTransferStateUseCase(
                    firstEmission,
                    PendingTransferState.SdkScanning
                )
            ) doSuspendableAnswer { yield() }

            underTest().test {
                assertThat(awaitItem()).isEqualTo(1)
                assertThat(awaitItem()).isEqualTo(3)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that uploadNodesUseCase is invoked for each file corresponding to the pending transfer`() =
        runTest {
            val pendingTransfers = (0..5).map { index ->
                val pendingTransferNodeIdentifier =
                    PendingTransferNodeIdentifier.CloudDriveNode(NodeId(4875L * index))
                mock<PendingTransfer> {
                    on { uriPath } doReturn UriPath("path/file$index.txt")
                    on { appData } doReturn listOf(mock<TransferAppData.ChatUpload>())
                    on { isHighPriority } doReturn (index == 2)
                    on { nodeIdentifier } doReturn pendingTransferNodeIdentifier
                }
            }

            stubNotSentPendingTransfers(pendingTransfers)

            underTest().test {
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }

            pendingTransfers.forEach { pendingTransfer ->
                verify(uploadFileUseCase).invoke(
                    uriPath = pendingTransfer.uriPath,
                    fileName = pendingTransfer.fileName,
                    appData = pendingTransfer.appData,
                    parentFolderId = pendingTransfer.nodeIdentifier.nodeId,
                    isHighPriority = pendingTransfer.isHighPriority,
                )
            }
        }


    @ParameterizedTest
    @MethodSource("provideTransferUpdatedAndScanningFinishEvents")
    fun `test that pending transfers state is updated to SdkScanned or AlreadyStarted when scanningFinished or updated event is received`(
        transferEvent: TransferEvent,
    ) = runTest {
        val pendingTransfer = mockPendingTransfer()
        stubNotSentPendingTransfers(listOf(pendingTransfer))

        whenever(
            uploadFileUseCase(
                uriPath = anyValueClass(),
                fileName = anyOrNull(),
                appData = anyOrNull(),
                parentFolderId = anyValueClass(),
                isHighPriority = anyOrNull(),
            )
        ) doReturn flowOf(transferEvent)

        underTest().test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        if (transferEvent.isTransferUpdated) {
            verify(updatePendingTransferStartedCountUseCase)(
                pendingTransfer,
                1,
                if (transferEvent.isAlreadyTransferredEvent) 1 else 0,
            )
        } else {
            verify(updatePendingTransferStateUseCase)(
                listOf(pendingTransfer),
                PendingTransferState.SdkScanned
            )
        }
    }

        @Test
        fun `test that pending transfers state is updated to ErrorStarting and failed completed transfer is added when there is an exception uploading the node`() =
            runTest {
                val pendingTransfer = mockPendingTransfer()
                stubNotSentPendingTransfers(listOf(pendingTransfer))

                val exception = RuntimeException()
                whenever(
                    uploadFileUseCase(
                        uriPath = anyValueClass(),
                        fileName = anyOrNull(),
                        appData = anyOrNull(),
                        parentFolderId = anyValueClass(),
                        isHighPriority = anyOrNull(),
                    )
                ) doReturn flow {
                    throw exception
                }

                underTest().test {
                    awaitItem()
                    cancelAndIgnoreRemainingEvents()
                }

                verify(updatePendingTransferStateUseCase)(
                    listOf(pendingTransfer),
                    PendingTransferState.ErrorStarting
                )
                verify(transferRepository).addCompletedTransferFromFailedPendingTransfer(
                    pendingTransfer,
                    0L,
                    exception,
                )
            }

        @Test
        fun `test that pending transfers state is updated to ErrorStarting and failed completed transfer is added when there is an exception collecting the pending transfers`() =
            runTest {
                val pendingTransfer = mock<PendingTransfer>()
                val pendingTransfers = listOf(pendingTransfer)
                val exception = RuntimeException()
                stubNotSentPendingTransfers(
                    pendingTransfers,
                    pendingTransfers,
                    emptyList()
                )
                whenever(
                    updatePendingTransferStateUseCase(
                        pendingTransfers,
                        PendingTransferState.SdkScanning
                    )
                ).doThrow(exception)

                underTest().test {
                    awaitComplete()
                }

                verify(updatePendingTransferStateUseCase)(
                    pendingTransfers,
                    PendingTransferState.ErrorStarting
                )
                verify(transferRepository).addCompletedTransferFromFailedPendingTransfer(
                    eq(pendingTransfer),
                    eq(0),
                    any(),
                )
            }

        @Test
        fun `test that active transfer is inserted when a Start event is received`() = runTest {
            val pendingTransfer = mockPendingTransfer()
            stubNotSentPendingTransfers(listOf(pendingTransfer))
            val transfer = mock<Transfer>()
            val transferEvent = mock<TransferEvent.TransferStartEvent> {
                on { this.transfer } doReturn transfer
            }
            whenever(
                uploadFileUseCase(
                    uriPath = anyValueClass(),
                    fileName = anyOrNull(),
                    appData = anyOrNull(),
                    parentFolderId = anyValueClass(),
                    isHighPriority = anyOrNull(),
                )
            ) doReturn flowOf(transferEvent)

            underTest().test {
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }

            verify(transferRepository)
                .insertOrUpdateActiveTransfer(transfer)
        }


    private fun mockPendingTransfer(): PendingTransfer {
        val pendingTransferNodeIdentifier = PendingTransferNodeIdentifier.CloudDriveNode(NodeId(8456L))
        return mock<PendingTransfer> {
            on { uriPath } doReturn UriPath("path/file.txt")
            on { appData } doReturn listOf(mock<TransferAppData.ChatUpload>())
            on { isHighPriority } doReturn false
            on { nodeIdentifier } doReturn pendingTransferNodeIdentifier
        }
    }

    private fun stubNotSentPendingTransfers(vararg pendingTransfers: List<PendingTransfer>) {
        whenever(
            getPendingTransfersByTypeAndStateUseCase(
                TransferType.GENERAL_UPLOAD,
                PendingTransferState.NotSentToSdk
            )
        ) doReturn flowOf(*pendingTransfers)
    }

    private fun provideTransferUpdatedAndScanningFinishEvents(): List<TransferEvent> {
        val fileTransfer = mock<Transfer> {
            on { this.isFolderTransfer } doReturn false
            on { this.isFinished } doReturn false
        }
        val fileTransferAlreadyUploaded = mock<Transfer> {
            on { this.isFolderTransfer } doReturn false
            on { isFinished } doReturn true
            on { transferredBytes } doReturn 0L
            on { state } doReturn TransferState.STATE_COMPLETED
        }
        val folderTransfer = mock<Transfer> {
            on { this.isFolderTransfer } doReturn false
        }
        return listOf(
            mock<TransferEvent.TransferUpdateEvent> {
                on { transfer } doReturn fileTransfer
            },
            mock<TransferEvent.TransferUpdateEvent> {
                on { transfer } doReturn fileTransferAlreadyUploaded
            },
            mock<TransferEvent.TransferFinishEvent> {
                on { transfer } doReturn folderTransfer
            },
            mock<TransferEvent.FolderTransferUpdateEvent> {
                on { stage } doReturn TransferStage.STAGE_TRANSFERRING_FILES
                on { transfer } doReturn folderTransfer
            },
            mock<TransferEvent.TransferFinishEvent> {
                on { transfer } doReturn fileTransfer
            },
            mock<TransferEvent.FolderTransferUpdateEvent> {
                on { stage } doReturn TransferStage.STAGE_TRANSFERRING_FILES
                on { transfer } doReturn folderTransfer
            },
            mock<TransferEvent.TransferStartEvent> {
                on { transfer } doReturn fileTransfer
            },
            mock<TransferEvent.TransferUpdateEvent> {
                on { transfer } doReturn folderTransfer
            },
        )
    }
}