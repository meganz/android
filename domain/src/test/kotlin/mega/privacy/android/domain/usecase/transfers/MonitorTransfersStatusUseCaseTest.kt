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
            underTest().test {
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

            underTest().test {
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

            underTest().test {
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

    private fun stubActiveTransfersFlows(paused: Boolean = false) =
        TransferType.entries.filterNot { it == TransferType.NONE }.associateWith { type ->
            MutableStateFlow(
                MonitorOngoingActiveTransfersResult(
                    activeTransferTotals = ActiveTransferTotals(
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
                    ),
                    paused = paused,
                    storageOverQuota = false,
                    transfersOverQuota = false,
                )
            ).also { flow ->
                whenever(monitorOngoingActiveTransfersUseCase(type)) doReturn flow
            }
        }

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
                    underTest.invokeLegacy().collect {
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
                    underTest.invokeLegacy().collect {
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
                    underTest.invokeLegacy().collect {
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
                    underTest.invokeLegacy().collect {
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
                    underTest.invokeLegacy().collect {
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
                underTest.invokeLegacy().test {
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
                underTest.invokeLegacy().test {
                    globalTransferFlow.emit(TransferEvent.TransferUpdateEvent(transfer))
                    val actual = awaitItem()

                    assertThat(actual.paused).isEqualTo(expected)
                }
            }
    }
}