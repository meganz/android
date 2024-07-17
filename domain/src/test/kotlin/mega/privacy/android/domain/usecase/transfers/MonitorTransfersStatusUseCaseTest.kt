package mega.privacy.android.domain.usecase.transfers

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.TransfersStatusInfo
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.MonitorOngoingActiveTransfersResult
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferStage
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.transfers.active.MonitorOngoingActiveTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.GetNumPendingDownloadsNonBackgroundUseCase
import mega.privacy.android.domain.usecase.transfers.paused.AreAllTransfersPausedUseCase
import mega.privacy.android.domain.usecase.transfers.uploads.GetNumPendingUploadsUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.math.BigInteger

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MonitorTransfersStatusUseCaseTest {
    private lateinit var underTest: MonitorTransfersStatusUseCase
    private val globalTransferFlow = MutableSharedFlow<TransferEvent>()
    private val transferRepository = mock<TransferRepository>()
    private val getNumPendingDownloadsNonBackgroundUseCase =
        mock<GetNumPendingDownloadsNonBackgroundUseCase>()
    private val getNumPendingUploadsUseCase = mock<GetNumPendingUploadsUseCase>()
    private val monitorOngoingActiveTransfersUseCase = mock<MonitorOngoingActiveTransfersUseCase>()
    private val areTransfersPaused = mock<AreAllTransfersPausedUseCase>()

    private val transfer = Transfer(
        transferType = TransferType.DOWNLOAD,
        transferredBytes = 2000L,
        totalBytes = 1000L,
        localPath = "localPath",
        parentPath = "parentPath",
        nodeHandle = 1L,
        parentHandle = 2L,
        fileName = "fileName",
        stage = TransferStage.STAGE_SCANNING,
        tag = 1,
        folderTransferTag = 2,
        speed = 1L,
        isSyncTransfer = false,
        isBackupTransfer = false,
        isForeignOverQuota = false,
        isStreamingTransfer = false,
        isFinished = false,
        isFolderTransfer = false,
        appData = emptyList(),
        state = TransferState.STATE_ACTIVE,
        priority = BigInteger.ONE,
        notificationNumber = 1L,
    )

    @BeforeAll
    fun setup() {
        underTest = MonitorTransfersStatusUseCase(
            repository = transferRepository,
            getNumPendingDownloadsNonBackgroundUseCase = getNumPendingDownloadsNonBackgroundUseCase,
            getNumPendingUploadsUseCase = getNumPendingUploadsUseCase,
            monitorOngoingActiveTransfersUseCase,
            areTransfersPaused,
        )
    }

    @BeforeEach
    fun resetMocks() = reset(
        transferRepository,
        getNumPendingDownloadsNonBackgroundUseCase,
        getNumPendingUploadsUseCase,
        monitorOngoingActiveTransfersUseCase,
        areTransfersPaused,
    )

    @Test
    fun `test that value with correct values is emitted when a new active transfer total is received`() =
        runTest {
            val flowsMap = stubActiveTransfersFlows()
            val expected = TransfersStatusInfo(
                totalSizeToTransfer = 100L,
                totalSizeTransferred = 200L,
                pendingUploads = 3,
                pendingDownloads = 4,
                paused = false,
                storageOverQuota = true,
                transferOverQuota = true,
            )
            underTest(uploadsWorkerFlag = true, activeTransfersInCameraUploadsFlag = true).test {
                awaitItem() //ignore initial
                flowsMap[TransferType.DOWNLOAD]?.update {
                    it.copy(
                        activeTransferTotals = it.activeTransferTotals.copy(
                            transferredBytes = expected.totalSizeTransferred,
                            totalBytes = expected.totalSizeToTransfer,
                            totalFileTransfers = expected.pendingDownloads,
                            totalFinishedFileTransfers = 0,
                        ),
                        transfersOverQuota = true,
                    )
                }
                awaitItem() //ignore updated value
                flowsMap[TransferType.GENERAL_UPLOAD]?.update {
                    it.copy(
                        activeTransferTotals = it.activeTransferTotals.copy(
                            totalFileTransfers = expected.pendingUploads,
                            totalFinishedFileTransfers = 0,
                        ),
                        storageOverQuota = true,
                    )
                }
                val actual = awaitItem()

                assertThat(actual).isEqualTo(expected)
            }
        }

    @Test
    fun `test that a new status with pause set to false is emitted when a single transfer type is not paused`() =
        runTest {
            val flowsMap = stubActiveTransfersFlows(paused = true)
            val expected = TransfersStatusInfo(paused = false)

            underTest(uploadsWorkerFlag = true, activeTransfersInCameraUploadsFlag = true).test {
                assertThat(awaitItem().paused).isTrue() // check initial is paused to validate the test is doing its job
                flowsMap.values.first().update {
                    it.copy(paused = false)
                }
                val actual = awaitItem()

                assertThat(actual).isEqualTo(expected)
            }
        }

    @Test
    fun `test that a new status with pause set to true is emitted when a all transfer types are paused`() =
        runTest {
            val flowsMap = stubActiveTransfersFlows()
            val expected = TransfersStatusInfo(paused = true)

            underTest(uploadsWorkerFlag = true, activeTransfersInCameraUploadsFlag = true).test {
                flowsMap.values.forEach { flow ->
                    assertThat(awaitItem().paused).isFalse() // all but last should be false, including initial
                    flow.update {
                        it.copy(paused = true)
                    }
                }
                val actual = awaitItem()

                assertThat(actual).isEqualTo(expected)
            }
        }

    @Test
    fun `test that totals are the addition of active transfers plus camera uploads when activeTransfersInCameraUploadsFlag is false`() =
        runTest {
            val expected = TransfersStatusInfo(
                totalSizeToTransfer = 2000L,
                totalSizeTransferred = 3000L,
                pendingUploads = 1,
                pendingDownloads = 4,
                paused = false,
                storageOverQuota = false,
                transferOverQuota = true,
            )

            whenever(getNumPendingUploadsUseCase()).thenReturn(expected.pendingUploads)
            whenever(transferRepository.monitorTransferEvents()).thenReturn(globalTransferFlow)
            whenever(getNumPendingDownloadsNonBackgroundUseCase()).thenReturn(0)
            whenever(areTransfersPaused()).thenReturn(false)

            val eventForLegacy = TransferEvent.TransferUpdateEvent(
                transfer.copy(transferType = TransferType.CU_UPLOAD)
            )
            val eventFromActiveTransfers =
                emptyActiveTransferTotals(TransferType.DOWNLOAD).copy(
                    transferredBytes = expected.totalSizeTransferred - transfer.transferredBytes,
                    totalBytes = expected.totalSizeToTransfer - transfer.totalBytes,
                    totalFileTransfers = expected.pendingDownloads,
                )

            val flowsMap = stubActiveTransfersFlows()
            underTest(
                uploadsWorkerFlag = true,
                activeTransfersInCameraUploadsFlag = false
            ).test {
                awaitItem() //ignore initial
                flowsMap[TransferType.DOWNLOAD]?.update {
                    it.copy(
                        activeTransferTotals = eventFromActiveTransfers,
                        transfersOverQuota = true,
                    )
                }
                awaitItem() //ignore updated value
                globalTransferFlow.emit(eventForLegacy)
                val actual = awaitItem()

                assertThat(actual).isEqualTo(expected)
            }
        }

    @Test
    fun `test that upload counter and paused are updated with legacy use cases on each new active transfer and each new transfer event when activeTransfersInCameraUploadsFlag is false`() =
        runTest {
            whenever(transferRepository.monitorTransferEvents()).thenReturn(globalTransferFlow)
            val flowsMap = stubActiveTransfersFlows()
            stubPendingUploadsAndPausedUseCases(0, false)
            underTest(
                uploadsWorkerFlag = true,
                activeTransfersInCameraUploadsFlag = false
            ).test {
                awaitItem().assertPendingUploadsAndPaused(0, false)

                stubPendingUploadsAndPausedUseCases(1, false)
                flowsMap[TransferType.DOWNLOAD]?.update {
                    it.copy(
                        activeTransferTotals = emptyActiveTransferTotals(TransferType.DOWNLOAD),
                        transfersOverQuota = true,
                    )
                }
                awaitItem().assertPendingUploadsAndPaused(1, false)

                stubPendingUploadsAndPausedUseCases(2, true)
                flowsMap[TransferType.GENERAL_UPLOAD]?.update {
                    it.copy(
                        activeTransferTotals = emptyActiveTransferTotals(TransferType.GENERAL_UPLOAD),
                        transfersOverQuota = true,
                    )
                }
                awaitItem().assertPendingUploadsAndPaused(2, true)

                stubPendingUploadsAndPausedUseCases(3, false)
                globalTransferFlow.emit(
                    TransferEvent.TransferUpdateEvent(
                        transfer.copy(transferType = TransferType.CU_UPLOAD)
                    )
                )
                awaitItem().assertPendingUploadsAndPaused(3, false)
            }
        }

    private suspend fun stubPendingUploadsAndPausedUseCases(uploads: Int, paused: Boolean) {
        whenever(getNumPendingUploadsUseCase()).thenReturn(uploads)
        whenever(areTransfersPaused()).thenReturn(paused)
    }

    private fun TransfersStatusInfo.assertPendingUploadsAndPaused(
        uploads: Int,
        paused: Boolean,
    ) {
        assertThat(this.pendingUploads).isEqualTo(uploads)
        assertThat(this.paused).isEqualTo(paused)
    }

    private fun stubActiveTransfersFlows(paused: Boolean = false) =
        TransferType.entries.filterNot { it == TransferType.NONE }.associateWith { type ->
            MutableStateFlow(
                MonitorOngoingActiveTransfersResult(
                    activeTransferTotals = emptyActiveTransferTotals(type),
                    paused = paused,
                    storageOverQuota = false,
                    transfersOverQuota = false,
                )
            ).also { flow ->
                whenever(monitorOngoingActiveTransfersUseCase(type)) doReturn flow
            }
        }

    private fun emptyActiveTransferTotals(type: TransferType) = ActiveTransferTotals(
        transfersType = type,
        totalTransfers = 1,
        totalFileTransfers = 0,
        pausedFileTransfers = 0,
        totalFinishedTransfers = 0,
        totalFinishedFileTransfers = 0,
        totalCompletedFileTransfers = 0,
        totalBytes = 0L,
        transferredBytes = 0L,
        totalAlreadyDownloadedFiles = 0,
    )

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class LegacyTests {

        @BeforeEach
        fun setUp() = runTest {
            whenever(transferRepository.monitorTransferEvents()).thenReturn(globalTransferFlow)
            whenever(getNumPendingDownloadsNonBackgroundUseCase()).thenReturn(0)
            whenever(getNumPendingUploadsUseCase()).thenReturn(0)
            whenever(areTransfersPaused()).thenReturn(false)
        }

        @Test
        fun `when monitorTransferEvents emit TransferStartEvent then the transfer size info equal to transfer size`() =
            runTest {
                val event = TransferEvent.TransferStartEvent(transfer)
                val transfersStatusInfo = MutableStateFlow(TransfersStatusInfo())
                val collectJob = launch(UnconfinedTestDispatcher()) {
                    underTest(
                        uploadsWorkerFlag = false,
                        activeTransfersInCameraUploadsFlag = false
                    ).collect {
                        transfersStatusInfo.value = it
                    }
                }
                globalTransferFlow.emit(event)
                assertThat(transfersStatusInfo.value.totalSizeToTransfer).isEqualTo(transfer.totalBytes)
                assertThat(transfersStatusInfo.value.totalSizeTransferred).isEqualTo(transfer.transferredBytes)
                collectJob.cancel()
            }

        @Test
        fun `when monitorTransferEvents emit TransferDataEvent then the transfer size info equal to transfer size`() =
            runTest {
                val event = TransferEvent.TransferDataEvent(transfer, ByteArray(0))
                val transfersStatusInfo = MutableStateFlow(TransfersStatusInfo())
                val collectJob = launch(UnconfinedTestDispatcher()) {
                    underTest(
                        uploadsWorkerFlag = false,
                        activeTransfersInCameraUploadsFlag = false
                    ).collect {
                        transfersStatusInfo.value = it
                    }
                }
                globalTransferFlow.emit(event)
                assertThat(transfersStatusInfo.value.totalSizeToTransfer).isEqualTo(transfer.totalBytes)
                assertThat(transfersStatusInfo.value.totalSizeTransferred).isEqualTo(transfer.transferredBytes)
                collectJob.cancel()
            }

        @Test
        fun `when monitorTransferEvents emit TransferFinishEvent then the transfer size info equal to transfer size`() =
            runTest {
                val event = TransferEvent.TransferFinishEvent(transfer, MegaException(-1, null))
                val transfersStatusInfo = MutableStateFlow(TransfersStatusInfo())
                val collectJob = launch(UnconfinedTestDispatcher()) {
                    underTest(
                        uploadsWorkerFlag = false,
                        activeTransfersInCameraUploadsFlag = false
                    ).collect {
                        transfersStatusInfo.value = it
                    }
                }
                globalTransferFlow.emit(event)
                assertThat(transfersStatusInfo.value.totalSizeToTransfer).isEqualTo(transfer.totalBytes)
                assertThat(transfersStatusInfo.value.totalSizeTransferred).isEqualTo(transfer.transferredBytes)
                collectJob.cancel()
            }

        @Test
        fun `when monitorTransferEvents emit 3 same TransferUpdateEvent then the transfer size info equal to transfer size`() =
            runTest {
                val event =
                    TransferEvent.TransferUpdateEvent(transfer)
                val transfersStatusInfo = MutableStateFlow(TransfersStatusInfo())
                val collectJob = launch(UnconfinedTestDispatcher()) {
                    underTest(
                        uploadsWorkerFlag = false,
                        activeTransfersInCameraUploadsFlag = false
                    ).collect {
                        transfersStatusInfo.value = it
                    }
                }
                globalTransferFlow.emit(event)
                globalTransferFlow.emit(event)
                globalTransferFlow.emit(event)
                assertThat(transfersStatusInfo.value.totalSizeToTransfer).isEqualTo(transfer.totalBytes)
                assertThat(transfersStatusInfo.value.totalSizeTransferred).isEqualTo(transfer.transferredBytes)
                collectJob.cancel()
            }

        @Test
        fun `when monitorTransferEvents emit 3 differ TransferUpdateEvent then the transfer size info equal to transfer size multiple 3`() =
            runTest {
                val eventOne =
                    TransferEvent.TransferUpdateEvent(transfer.copy(tag = 1))
                val eventTwo =
                    TransferEvent.TransferUpdateEvent(transfer.copy(tag = 2))
                val eventThree =
                    TransferEvent.TransferUpdateEvent(transfer.copy(tag = 3))
                val transfersStatusInfo = MutableStateFlow(TransfersStatusInfo())
                val collectJob = launch(UnconfinedTestDispatcher()) {
                    underTest(
                        uploadsWorkerFlag = false,
                        activeTransfersInCameraUploadsFlag = false
                    ).collect {
                        transfersStatusInfo.value = it
                    }
                }
                globalTransferFlow.emit(eventOne)
                globalTransferFlow.emit(eventTwo)
                globalTransferFlow.emit(eventThree)
                assertThat(transfersStatusInfo.value.totalSizeToTransfer).isEqualTo(transfer.totalBytes * 3)
                assertThat(transfersStatusInfo.value.totalSizeTransferred).isEqualTo(transfer.transferredBytes * 3)
                collectJob.cancel()
            }

        @Test
        fun `test that pending transfers are set correctly from the proper use cases when a new event is emitted`() =
            runTest {
                val expectedPendingDownloads = 3
                val expectedPendingUploads = 4
                whenever(getNumPendingUploadsUseCase()) doReturn expectedPendingUploads
                whenever(getNumPendingDownloadsNonBackgroundUseCase()) doReturn expectedPendingDownloads
                underTest(
                    uploadsWorkerFlag = false,
                    activeTransfersInCameraUploadsFlag = false
                ).test {
                    awaitItem() //ignore first
                    globalTransferFlow.emit(TransferEvent.TransferUpdateEvent(transfer))
                    val actual = awaitItem()

                    assertThat(actual.pendingDownloads).isEqualTo(expectedPendingDownloads)
                    assertThat(actual.pendingUploads).isEqualTo(expectedPendingUploads)
                }
            }

        @Test
        fun `test that paused are set correctly from the proper use case when a new event is emitted`() =
            runTest {
                val expected = true
                whenever(areTransfersPaused()) doReturn expected
                underTest(
                    uploadsWorkerFlag = false,
                    activeTransfersInCameraUploadsFlag = false
                ).test {
                    awaitItem() //ignore first
                    globalTransferFlow.emit(TransferEvent.TransferUpdateEvent(transfer))
                    val actual = awaitItem()

                    assertThat(actual.paused).isEqualTo(expected)
                }
            }
    }
}