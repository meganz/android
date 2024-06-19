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
import mega.privacy.android.domain.entity.TransfersSizeInfo
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferStage
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.transfers.downloads.GetNumPendingDownloadsNonBackgroundUseCase
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
internal class MonitorTransfersSizeUseCaseTest {
    private lateinit var underTest: MonitorTransfersSizeUseCase
    private val globalTransferFlow = MutableSharedFlow<TransferEvent>()
    private val transferRepository = mock<TransferRepository>()
    private val getNumPendingDownloadsNonBackgroundUseCase =
        mock<GetNumPendingDownloadsNonBackgroundUseCase>()
    private val getNumPendingUploadsUseCase = mock<GetNumPendingUploadsUseCase>()
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
        underTest = MonitorTransfersSizeUseCase(
            repository = transferRepository,
            getNumPendingDownloadsNonBackgroundUseCase = getNumPendingDownloadsNonBackgroundUseCase,
            getNumPendingUploadsUseCase = getNumPendingUploadsUseCase,
        )
    }

    @Test
    fun `test that value with correct values is emitted when a new active transfer total is received`() =
        runTest {
            val flowsMap =
                TransferType.entries.filterNot { it == TransferType.NONE }.associateWith { type ->
                    MutableStateFlow(
                        ActiveTransferTotals(
                            transfersType = type,
                            totalTransfers = 0,
                            totalFileTransfers = 0,
                            pausedFileTransfers = 0,
                            totalFinishedTransfers = 0,
                            totalFinishedFileTransfers = 0,
                            totalCompletedFileTransfers = 0,
                            totalBytes = 0L,
                            transferredBytes = 0L,
                            totalAlreadyDownloadedFiles = 0,
                        )
                    ).also { flow ->
                        whenever(transferRepository.getActiveTransferTotalsByType(type)) doReturn flow
                    }
                }
            val expected = TransfersSizeInfo(
                totalSizeToTransfer = 100L,
                totalSizeTransferred = 200L,
                pendingUploads = 3,
                pendingDownloads = 4,
            )
            underTest().test {
                awaitItem() //ignore initial
                flowsMap[TransferType.DOWNLOAD]?.update {
                    it.copy(
                        transferredBytes = expected.totalSizeTransferred,
                        totalBytes = expected.totalSizeToTransfer,
                        totalFileTransfers = expected.pendingDownloads ?: -1,
                        totalFinishedFileTransfers = 0,
                    )
                }
                awaitItem() //ignore updated value
                flowsMap[TransferType.GENERAL_UPLOAD]?.update {
                    it.copy(
                        totalFileTransfers = expected.pendingUploads ?: -1,
                        totalFinishedFileTransfers = 0,
                    )
                }
                val actual = awaitItem()

                assertThat(actual).isEqualTo(expected)
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
        }

        @Test
        fun `when monitorTransferEvents emit TransferStartEvent then the transfer size info equal to transfer size`() =
            runTest {
                val event = TransferEvent.TransferStartEvent(transfer)
                val transfersSizeInfo = MutableStateFlow(TransfersSizeInfo())
                val collectJob = launch(UnconfinedTestDispatcher()) {
                    underTest.invokeLegacy().collect {
                        transfersSizeInfo.value = it
                    }
                }
                globalTransferFlow.emit(event)
                assertThat(transfersSizeInfo.value.totalSizeToTransfer).isEqualTo(transfer.totalBytes)
                assertThat(transfersSizeInfo.value.totalSizeTransferred).isEqualTo(transfer.transferredBytes)
                collectJob.cancel()
            }

        @Test
        fun `when monitorTransferEvents emit TransferDataEvent then the transfer size info equal to transfer size`() =
            runTest {
                val event = TransferEvent.TransferDataEvent(transfer, ByteArray(0))
                val transfersSizeInfo = MutableStateFlow(TransfersSizeInfo())
                val collectJob = launch(UnconfinedTestDispatcher()) {
                    underTest.invokeLegacy().collect {
                        transfersSizeInfo.value = it
                    }
                }
                globalTransferFlow.emit(event)
                assertThat(transfersSizeInfo.value.totalSizeToTransfer).isEqualTo(transfer.totalBytes)
                assertThat(transfersSizeInfo.value.totalSizeTransferred).isEqualTo(transfer.transferredBytes)
                collectJob.cancel()
            }

        @Test
        fun `when monitorTransferEvents emit TransferFinishEvent then the transfer size info equal to transfer size`() =
            runTest {
                val event = TransferEvent.TransferFinishEvent(transfer, MegaException(-1, null))
                val transfersSizeInfo = MutableStateFlow(TransfersSizeInfo())
                val collectJob = launch(UnconfinedTestDispatcher()) {
                    underTest.invokeLegacy().collect {
                        transfersSizeInfo.value = it
                    }
                }
                globalTransferFlow.emit(event)
                assertThat(transfersSizeInfo.value.totalSizeToTransfer).isEqualTo(transfer.totalBytes)
                assertThat(transfersSizeInfo.value.totalSizeTransferred).isEqualTo(transfer.transferredBytes)
                collectJob.cancel()
            }

        @Test
        fun `when monitorTransferEvents emit 3 same TransferUpdateEvent then the transfer size info equal to transfer size`() =
            runTest {
                val event =
                    TransferEvent.TransferUpdateEvent(transfer)
                val transfersSizeInfo = MutableStateFlow(TransfersSizeInfo())
                val collectJob = launch(UnconfinedTestDispatcher()) {
                    underTest.invokeLegacy().collect {
                        transfersSizeInfo.value = it
                    }
                }
                globalTransferFlow.emit(event)
                globalTransferFlow.emit(event)
                globalTransferFlow.emit(event)
                assertThat(transfersSizeInfo.value.totalSizeToTransfer).isEqualTo(transfer.totalBytes)
                assertThat(transfersSizeInfo.value.totalSizeTransferred).isEqualTo(transfer.transferredBytes)
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
                val transfersSizeInfo = MutableStateFlow(TransfersSizeInfo())
                val collectJob = launch(UnconfinedTestDispatcher()) {
                    underTest.invokeLegacy().collect {
                        transfersSizeInfo.value = it
                    }
                }
                globalTransferFlow.emit(eventOne)
                globalTransferFlow.emit(eventTwo)
                globalTransferFlow.emit(eventThree)
                assertThat(transfersSizeInfo.value.totalSizeToTransfer).isEqualTo(transfer.totalBytes * 3)
                assertThat(transfersSizeInfo.value.totalSizeTransferred).isEqualTo(transfer.transferredBytes * 3)
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
    }
}