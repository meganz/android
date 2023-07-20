package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.TransfersSizeInfo
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferStage
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.repository.TransferRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigInteger
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
internal class DefaultMonitorTransfersSizeTest {
    private lateinit var underTest: MonitorTransfersSize
    private val globalTransferFlow = MutableSharedFlow<TransferEvent>()
    private val transferRepository = mock<TransferRepository>()
    private val transfer = Transfer(
        transferType = TransferType.TYPE_DOWNLOAD,
        transferredBytes = 2000L,
        totalBytes = 1000L,
        localPath = "localPath",
        parentPath = "parentPath",
        nodeHandle = 1L,
        parentHandle = 2L,
        fileName = "fileName",
        stage = TransferStage.STAGE_SCANNING,
        tag = 1,
        speed = 1L,
        isForeignOverQuota = false,
        isStreamingTransfer = false,
        isFinished = false,
        isFolderTransfer = false,
        appData = "",
        transferAppData = emptyList(),
        state = TransferState.STATE_ACTIVE,
        priority = BigInteger.ONE,
        notificationNumber = 1L,
    )

    @Before
    fun setUp() {
        whenever(transferRepository.monitorTransferEvents()).thenReturn(globalTransferFlow)
    }

    @Test
    fun `when monitorTransferEvents emit Upload TransferUpdateEvent then the transfer size info equal to transfer size and transferType is TYPE_UPLOAD`() =
        runTest {
            val event =
                TransferEvent.TransferUpdateEvent(transfer.copy(transferType = TransferType.TYPE_UPLOAD))
            underTest = DefaultMonitorTransfersSize(
                repository = transferRepository
            )
            val transfersSizeInfo = MutableStateFlow(TransfersSizeInfo())
            val collectJob = launch(UnconfinedTestDispatcher()) {
                underTest().collect {
                    transfersSizeInfo.value = it
                }
            }
            globalTransferFlow.emit(event)
            assertEquals(transfersSizeInfo.value.totalSizePendingTransfer, transfer.totalBytes)
            assertEquals(transfersSizeInfo.value.totalSizeTransferred, transfer.transferredBytes)
            assertEquals(transfersSizeInfo.value.transferType, TransferType.TYPE_UPLOAD)
            collectJob.cancel()
        }

    @Test
    fun `when monitorTransferEvents emit Download TransferUpdateEvent then the transfer size info equal to transfer size and transferType is TYPE_DOWNLOAD`() =
        runTest {
            val event =
                TransferEvent.TransferUpdateEvent(transfer.copy(transferType = TransferType.TYPE_DOWNLOAD))
            underTest = DefaultMonitorTransfersSize(
                repository = transferRepository
            )
            val transfersSizeInfo = MutableStateFlow(TransfersSizeInfo())
            val collectJob = launch(UnconfinedTestDispatcher()) {
                underTest().collect {
                    transfersSizeInfo.value = it
                }
            }
            globalTransferFlow.emit(event)
            assertEquals(transfersSizeInfo.value.totalSizePendingTransfer, transfer.totalBytes)
            assertEquals(transfersSizeInfo.value.totalSizeTransferred, transfer.transferredBytes)
            assertEquals(transfersSizeInfo.value.transferType, TransferType.TYPE_DOWNLOAD)
            collectJob.cancel()
        }

    @Test
    fun `when monitorTransferEvents emit TransferStartEvent then the transfer size info equal to transfer size`() =
        runTest {
            val event = TransferEvent.TransferStartEvent(transfer)
            underTest = DefaultMonitorTransfersSize(
                repository = transferRepository
            )
            val transfersSizeInfo = MutableStateFlow(TransfersSizeInfo())
            val collectJob = launch(UnconfinedTestDispatcher()) {
                underTest().collect {
                    transfersSizeInfo.value = it
                }
            }
            globalTransferFlow.emit(event)
            assertEquals(transfersSizeInfo.value.totalSizePendingTransfer, transfer.totalBytes)
            assertEquals(transfersSizeInfo.value.totalSizeTransferred, transfer.transferredBytes)
            assertEquals(transfersSizeInfo.value.transferType, transfer.transferType)
            collectJob.cancel()
        }

    @Test
    fun `when monitorTransferEvents emit TransferDataEvent then the transfer size info equal to transfer size`() =
        runTest {
            val event = TransferEvent.TransferDataEvent(transfer, ByteArray(0))
            underTest = DefaultMonitorTransfersSize(
                repository = transferRepository
            )
            val transfersSizeInfo = MutableStateFlow(TransfersSizeInfo())
            val collectJob = launch(UnconfinedTestDispatcher()) {
                underTest().collect {
                    transfersSizeInfo.value = it
                }
            }
            globalTransferFlow.emit(event)
            assertEquals(transfersSizeInfo.value.totalSizePendingTransfer, transfer.totalBytes)
            assertEquals(transfersSizeInfo.value.totalSizeTransferred, transfer.transferredBytes)
            assertEquals(transfersSizeInfo.value.transferType, transfer.transferType)
            collectJob.cancel()
        }

    @Test
    fun `when monitorTransferEvents emit TransferFinishEvent then the transfer size info equal to transfer size`() =
        runTest {
            val event = TransferEvent.TransferFinishEvent(transfer, MegaException(-1, null))
            underTest = DefaultMonitorTransfersSize(
                repository = transferRepository
            )
            val transfersSizeInfo = MutableStateFlow(TransfersSizeInfo())
            val collectJob = launch(UnconfinedTestDispatcher()) {
                underTest().collect {
                    transfersSizeInfo.value = it
                }
            }
            globalTransferFlow.emit(event)
            assertEquals(transfersSizeInfo.value.totalSizePendingTransfer, transfer.totalBytes)
            assertEquals(transfersSizeInfo.value.totalSizeTransferred, transfer.transferredBytes)
            assertEquals(transfersSizeInfo.value.transferType, transfer.transferType)
            collectJob.cancel()
        }

    @Test
    fun `when monitorTransferEvents emit 3 same TransferUpdateEvent then the transfer size info equal to transfer size`() =
        runTest {
            val event =
                TransferEvent.TransferUpdateEvent(transfer)
            underTest = DefaultMonitorTransfersSize(
                repository = transferRepository
            )
            val transfersSizeInfo = MutableStateFlow(TransfersSizeInfo())
            val collectJob = launch(UnconfinedTestDispatcher()) {
                underTest().collect {
                    transfersSizeInfo.value = it
                }
            }
            globalTransferFlow.emit(event)
            globalTransferFlow.emit(event)
            globalTransferFlow.emit(event)
            assertEquals(transfersSizeInfo.value.totalSizePendingTransfer, transfer.totalBytes)
            assertEquals(transfersSizeInfo.value.totalSizeTransferred, transfer.transferredBytes)
            assertEquals(transfersSizeInfo.value.transferType, transfer.transferType)
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
            underTest = DefaultMonitorTransfersSize(
                repository = transferRepository
            )
            val transfersSizeInfo = MutableStateFlow(TransfersSizeInfo())
            val collectJob = launch(UnconfinedTestDispatcher()) {
                underTest().collect {
                    transfersSizeInfo.value = it
                }
            }
            globalTransferFlow.emit(eventOne)
            globalTransferFlow.emit(eventTwo)
            globalTransferFlow.emit(eventThree)
            assertEquals(transfersSizeInfo.value.totalSizePendingTransfer, transfer.totalBytes * 3)
            assertEquals(
                transfersSizeInfo.value.totalSizeTransferred,
                transfer.transferredBytes * 3
            )
            assertEquals(transfersSizeInfo.value.transferType, transfer.transferType)
            collectJob.cancel()
        }
}