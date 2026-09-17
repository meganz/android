package mega.privacy.android.domain.usecase.transfers.pending

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import mega.privacy.android.domain.entity.node.DefaultTypedFileNode
import mega.privacy.android.domain.entity.transfer.DownloadBatchItem
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferQuotaState
import mega.privacy.android.domain.entity.transfer.TransferStage
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.entity.transfer.isAlreadyTransferredEvent
import mega.privacy.android.domain.entity.transfer.isTransferUpdated
import mega.privacy.android.domain.entity.transfer.pending.PendingTransfer
import mega.privacy.android.domain.entity.transfer.pending.PendingTransferState
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.exception.node.NodeDoesNotExistsException
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.transfers.overquota.BroadcastTransferQuotaStateUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.isA
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StartAllPendingDownloadsUseCaseTest {
    private lateinit var underTest: StartAllPendingDownloadsUseCase

    private val transferRepository = mock<TransferRepository>()
    private val getPendingTransfersByTypeAndStateUseCase =
        mock<GetPendingTransfersByTypeAndStateUseCase>()
    private val updatePendingTransferStateUseCase = mock<UpdatePendingTransferStateUseCase>()
    private val getTypedNodeFromPendingTransferUseCase =
        mock<GetTypedNodeFromPendingTransferUseCase>()
    private val updatePendingTransferStartedCountUseCase =
        mock<UpdatePendingTransferStartedCountUseCase>()
    private val broadcastTransferQuotaStateUseCase = mock<BroadcastTransferQuotaStateUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = StartAllPendingDownloadsUseCase(
            transferRepository,
            getPendingTransfersByTypeAndStateUseCase,
            updatePendingTransferStateUseCase,
            updatePendingTransferStartedCountUseCase,
            getTypedNodeFromPendingTransferUseCase,
            broadcastTransferQuotaStateUseCase,
        )
    }

    @BeforeEach
    fun cleanUp() {
        reset(
            transferRepository,
            getPendingTransfersByTypeAndStateUseCase,
            updatePendingTransferStateUseCase,
            getTypedNodeFromPendingTransferUseCase,
            updatePendingTransferStartedCountUseCase,
            broadcastTransferQuotaStateUseCase,
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
            val pendingTransfers = listOf(stubPendingTransfer())
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
    fun `test that a batch item is started for each node corresponding to the pending transfer`() =
        runTest {
            val pendingTransfers = (0..5).map { index ->
                stubPendingTransferInGroup(index = index, groupId = 1L)
            }
            val nodes = pendingTransfers.map { pendingTransfer ->
                mock<DefaultTypedFileNode>().also {
                    whenever(getTypedNodeFromPendingTransferUseCase(pendingTransfer)) doReturn it
                }
            }
            stubNotSentPendingTransfers(pendingTransfers)
            stubBatchEvents()

            underTest().test {
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }

            val batch = argumentCaptor<List<DownloadBatchItem>>()
            verify(transferRepository).startDownloadBatch(batch.capture())
            assertThat(batch.firstValue).isEqualTo(
                pendingTransfers.mapIndexed { index, pendingTransfer ->
                    DownloadBatchItem(
                        node = nodes[index],
                        destinationPath = pendingTransfer.uriPath.value,
                        fileName = pendingTransfer.fileName,
                        appData = pendingTransfer.appData,
                        isHighPriority = pendingTransfer.isHighPriority,
                    )
                }
            )
        }

    @Test
    fun `test that a single batch is started per transfer group`() = runTest {
        val pendingTransfers = (0..3).map { index ->
            stubPendingTransferInGroup(index = index, groupId = if (index < 2) 1L else 2L)
        }
        stubNotSentPendingTransfers(pendingTransfers)
        stubBatchEvents()

        underTest().test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        val batches = argumentCaptor<List<DownloadBatchItem>>()
        verify(transferRepository, times(2)).startDownloadBatch(batches.capture())
        assertThat(batches.allValues.map { it.size }).containsExactly(2, 2)
    }

    @Test
    fun `test that the predicted transfer quota state is broadcast when the batch reports one`() =
        runTest {
            val pendingTransfer = stubPendingTransferInGroup(index = 0, groupId = 1L)
            stubNotSentPendingTransfers(listOf(pendingTransfer))
            val batchTransfer = mock<Transfer> {
                on { appData } doReturn emptyList()
            }
            val quotaStateEvent = mock<TransferEvent.TransferQuotaStateEvent> {
                on { transfer } doReturn batchTransfer
                on { state } doReturn TransferQuotaState.Orange
            }
            stubBatchEvents(quotaStateEvent)

            underTest().test {
                awaitItem()
                // The batch is collected by a child of this flow, so it must be left to finish.
                awaitComplete()
            }

            verify(broadcastTransferQuotaStateUseCase).invoke(TransferQuotaState.Orange)
        }


    @ParameterizedTest
    @MethodSource("provideTransferUpdatedAndScanningFinishEvents")
    fun `test that pending transfers state is updated to SdkScanned or AlreadyStarted when scanningFinished  or updated event is received`(
        transferEvent: TransferEvent
    ) = runTest {
        val pendingTransfer = stubPendingTransfer()
        stubNotSentPendingTransfers(listOf(pendingTransfer))
        val typedNode = mock<DefaultTypedFileNode>()
        whenever(getTypedNodeFromPendingTransferUseCase(pendingTransfer)) doReturn typedNode
        stubBatchEvents(transferEvent)

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
    fun `test that pending transfers state is updated to ErrorStarting and failed completed transfer is added when there is an exception getting the node`() =
        runTest {
            val pendingTransfer = stubPendingTransfer()
            stubNotSentPendingTransfers(listOf(pendingTransfer))
            val exception = RuntimeException()
            whenever(getTypedNodeFromPendingTransferUseCase(pendingTransfer)) doThrow exception

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
                0,
                exception,
            )
        }

    @Test
    fun `test that pending transfers state is updated to ErrorStarting and failed completed transfer is added when node does not exist`() =
        runTest {
            val pendingTransfer = stubPendingTransfer()
            stubNotSentPendingTransfers(listOf(pendingTransfer))
            whenever(getTypedNodeFromPendingTransferUseCase(pendingTransfer)) doReturn null

            underTest().test {
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }

            verify(updatePendingTransferStateUseCase)(
                listOf(pendingTransfer),
                PendingTransferState.ErrorStarting
            )
            verify(transferRepository).addCompletedTransferFromFailedPendingTransfer(
                eq(pendingTransfer),
                eq(0),
                isA<NodeDoesNotExistsException>(),
            )
        }

    @Test
    fun `test that pending transfers state is updated to ErrorStarting and failed completed transfer is added when there is an exception downloading the node`() =
        runTest {
            val pendingTransfer = stubPendingTransfer()
            stubNotSentPendingTransfers(listOf(pendingTransfer))
            val typedNode = mock<DefaultTypedFileNode>()
            val exception = RuntimeException()
            whenever(getTypedNodeFromPendingTransferUseCase(pendingTransfer)) doReturn typedNode
            whenever(transferRepository.startDownloadBatch(any())) doReturn flow {
                throw exception
            }

            underTest().test {
                awaitItem()
                awaitComplete()
            }

            verify(updatePendingTransferStateUseCase)(
                listOf(pendingTransfer),
                PendingTransferState.ErrorStarting
            )
            verify(transferRepository).addCompletedTransferFromFailedPendingTransfer(
                eq(pendingTransfer),
                eq(0L),
                eq(exception),
            )
        }

    @Test
    fun `test that state is updated to ErrorStarting and failed completed transfer is added on collection error`() =
        runTest {
            val pendingTransfer = stubPendingTransfer()
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
    fun `test that state is updated to ErrorStarting and failed completed transfer is not added on collection error for preview transfer`() =
        runTest {
            val pendingTransfer = mock<PendingTransfer> {
                on { appData } doReturn listOf(TransferAppData.PreviewDownload)
            }
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
            verify(transferRepository, never()).addCompletedTransferFromFailedPendingTransfer(
                any(),
                any(),
                any()
            )
        }

    @Test
    fun `test that active transfer is inserted when a Start event is received`() = runTest {
        val pendingTransfer = stubPendingTransfer()
        stubNotSentPendingTransfers(listOf(pendingTransfer))
        val typedNode = mock<DefaultTypedFileNode>()
        val transfer = mock<Transfer> {
            on { appData } doReturn emptyList()
        }
        val transferEvent = mock<TransferEvent.TransferStartEvent> {
            on { this.transfer } doReturn transfer
        }
        whenever(getTypedNodeFromPendingTransferUseCase(pendingTransfer)) doReturn typedNode
        stubBatchEvents(transferEvent)

        underTest().test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }

        verify(transferRepository)
            .putActiveTransfer(transfer)
    }


    private fun stubPendingTransfer() = mock<PendingTransfer> {
        on { pendingTransferId } doReturn 0L
        on { uriPath } doReturn UriPath("path/file.txt")
    }

    private fun stubBatchEvents(vararg events: TransferEvent) {
        whenever(transferRepository.startDownloadBatch(any())) doReturn flowOf(*events)
    }

    private fun stubPendingTransferInGroup(index: Int, groupId: Long): PendingTransfer {
        val pendingTransfer = mock<PendingTransfer> {
            on { pendingTransferId } doReturn index.toLong()
            on { uriPath } doReturn UriPath("path/file$index.txt")
            on { fileName } doReturn "file$index.txt"
            on { appData } doReturn listOf(TransferAppData.TransferGroup(groupId))
            on { isHighPriority } doReturn false
        }
        getTypedNodeFromPendingTransferUseCase.stub {
            onBlocking { invoke(pendingTransfer) } doReturn mock<DefaultTypedFileNode>()
        }
        return pendingTransfer
    }

    private fun stubNotSentPendingTransfers(vararg pendingTransfers: List<PendingTransfer>) {
        whenever(
            getPendingTransfersByTypeAndStateUseCase(
                TransferType.DOWNLOAD,
                PendingTransferState.NotSentToSdk
            )
        ) doReturn flowOf(*pendingTransfers)
    }

    private fun provideTransferUpdatedAndScanningFinishEvents(): List<TransferEvent> {
        val subTransferAppData = emptyList<TransferAppData>()
        val fileTransfer = mock<Transfer> {
            on { this.isFolderTransfer } doReturn false
            on { this.isFinished } doReturn false
            on { appData } doReturn subTransferAppData
        }
        val fileTransferAlreadyDownloaded = mock<Transfer> {
            on { this.isFolderTransfer } doReturn false
            on { isFinished } doReturn true
            on { transferredBytes } doReturn 0L
            on { state } doReturn TransferState.STATE_COMPLETED
            on { appData } doReturn subTransferAppData
        }
        val folderTransfer = mock<Transfer> {
            on { this.isFolderTransfer } doReturn false
            on { appData } doReturn subTransferAppData
        }
        return listOf(
            mock<TransferEvent.TransferUpdateEvent> {
                on { transfer } doReturn fileTransfer
            },
            mock<TransferEvent.TransferUpdateEvent> {
                on { transfer } doReturn fileTransferAlreadyDownloaded
            },
            mock<TransferEvent.TransferFinishEvent> {
                on { transfer } doReturn folderTransfer
            },
            mock<TransferEvent.FolderTransferUpdateEvent> {
                on { stage } doReturn TransferStage.STAGE_TRANSFERRING_FILES
                on { transfer } doReturn folderTransfer
            },
            mock<TransferEvent.TransferFinishEvent>{
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