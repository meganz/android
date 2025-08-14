package mega.privacy.android.domain.usecase.transfers.active

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.exception.BusinessAccountExpiredMegaException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.usecase.qrcode.ScanMediaFileUseCase
import mega.privacy.android.domain.usecase.transfers.downloads.HandleAvailableOfflineEventUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.BroadcastTransferOverQuotaUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HandleDownloadTransferEventsUseCaseTest {
    private lateinit var underTest: HandleDownloadTransferEventsUseCase

    private val scanMediaFileUseCase = mock<ScanMediaFileUseCase>()
    private val handleAvailableOfflineEventUseCase = mock<HandleAvailableOfflineEventUseCase>()
    private val broadcastTransferOverQuotaUseCase = mock<BroadcastTransferOverQuotaUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = HandleDownloadTransferEventsUseCase(
            scanMediaFileUseCase = scanMediaFileUseCase,
            handleAvailableOfflineEventUseCase = handleAvailableOfflineEventUseCase,
            broadcastTransferOverQuotaUseCase = broadcastTransferOverQuotaUseCase,
        )
    }

    @BeforeEach
    fun cleanUp() {
        reset(
            scanMediaFileUseCase,
            handleAvailableOfflineEventUseCase,
            broadcastTransferOverQuotaUseCase,
        )
    }

    @Test
    fun `test that scan media file use case is invoked when a finish transfer event is received`() =
        runTest {
            val bytes = 38754L
            val localPath = "path"
            val transfer = mock<Transfer> {
                on { this.transferType } doReturn TransferType.DOWNLOAD
                on { this.transferredBytes } doReturn bytes
                on { this.totalBytes } doReturn bytes
                on { this.localPath } doReturn localPath
            }
            val finishEvent = TransferEvent.TransferFinishEvent(transfer, null)

            underTest(finishEvent)

            verify(scanMediaFileUseCase).invoke(
                arrayOf(localPath), arrayOf("")
            )
        }

    @Test
    fun `test that scan media file use case is invoked when multiple finish transfer event are received`() =
        runTest {
            val bytes = 38754L
            val localPath = "path"
            val transfer = mock<Transfer> {
                on { this.transferType } doReturn TransferType.DOWNLOAD
                on { this.transferredBytes } doReturn bytes
                on { this.totalBytes } doReturn bytes
                on { this.localPath } doReturn localPath
            }
            val finishEvent = TransferEvent.TransferFinishEvent(transfer, null)

            underTest(finishEvent, finishEvent)

            verify(scanMediaFileUseCase).invoke(
                arrayOf(localPath, localPath), arrayOf("")
            )
        }

    @Test
    fun `test that not download transfer events are filtered out`() =
        runTest {
            val bytes = 38754L
            val localPath = "path"
            val transfer = mock<Transfer> {
                on { this.transferType } doReturn TransferType.GENERAL_UPLOAD
                on { this.transferredBytes } doReturn bytes
                on { this.totalBytes } doReturn bytes
                on { this.localPath } doReturn localPath
            }
            val finishEvent = TransferEvent.TransferFinishEvent(transfer, null)

            underTest(finishEvent)

            verifyNoInteractions(scanMediaFileUseCase)
        }

    @Test
    fun `test that not transfer finish events are filtered out`() =
        runTest {
            val bytes = 38754L
            val localPath = "path"
            val transfer = mock<Transfer> {
                on { this.transferType } doReturn TransferType.DOWNLOAD
                on { this.transferredBytes } doReturn bytes
                on { this.totalBytes } doReturn bytes
                on { this.localPath } doReturn localPath
            }
            val finishEvent = TransferEvent.TransferUpdateEvent(transfer)

            underTest(finishEvent)

            verifyNoInteractions(scanMediaFileUseCase)
        }

    @Test
    fun `test that not finished transfer finish events are filtered out`() =
        runTest {
            val bytes = 38754L
            val localPath = "path"
            val transfer = mock<Transfer> {
                on { this.transferType } doReturn TransferType.DOWNLOAD
                on { this.transferredBytes } doReturn bytes - 1
                on { this.totalBytes } doReturn bytes
                on { this.localPath } doReturn localPath
            }
            val finishEvent = TransferEvent.TransferFinishEvent(transfer, null)

            underTest(finishEvent)

            verifyNoInteractions(scanMediaFileUseCase)
        }

    @ParameterizedTest
    @MethodSource("provideFinishEvents")
    fun `test that handleAvailableOfflineEventUseCase is invoked when finish event is received`(
        transferEvent: TransferEvent.TransferFinishEvent,
    ) = runTest {
        underTest(transferEvent)
        verify(handleAvailableOfflineEventUseCase).invoke(transferEvent)
    }

    @Test
    fun `test that broadcastTransferOverQuotaUseCase is invoked when a QuotaExceededMegaException is received as a temporal error for download Event`() =
        runTest {
            val transfer = mock<Transfer> {
                on { this.transferType }.thenReturn(TransferType.DOWNLOAD)
            }
            val transferEvent = mock<TransferEvent.TransferTemporaryErrorEvent> {
                on { this.transfer }.thenReturn(transfer)
                on { this.error }.thenReturn(QuotaExceededMegaException(1, value = 1))
            }
            underTest.invoke(transferEvent)
            verify(broadcastTransferOverQuotaUseCase).invoke(true)
        }

    @Test
    fun `test that broadcastTransferOverQuotaUseCase is invoked with parameter equals to false when a Start event is received for download Event`() =
        runTest {
            val transfer = mock<Transfer> {
                on { this.transferType }.thenReturn(TransferType.DOWNLOAD)
            }
            val transferEvent = mock<TransferEvent.TransferStartEvent> {
                on { this.transfer }.thenReturn(transfer)
            }
            underTest.invoke(transferEvent)
            verify(broadcastTransferOverQuotaUseCase).invoke(false)
        }

    @Test
    fun `test that broadcastTransferOverQuotaUseCase is invoked with parameter equals to false when a Update event is received for download Event`() =
        runTest {
            val transfer = mock<Transfer> {
                on { this.transferType }.thenReturn(TransferType.DOWNLOAD)
            }
            val transferEvent = mock<TransferEvent.TransferUpdateEvent> {
                on { this.transfer }.thenReturn(transfer)
            }
            underTest.invoke(transferEvent)
            verify(broadcastTransferOverQuotaUseCase).invoke(false)
        }

    private fun provideFinishEvents(): List<TransferEvent.TransferFinishEvent> = buildList {
        TransferType.entries.forEach { transferType ->
            val transfer = mock<Transfer> {
                on { this.transferType }.thenReturn(transferType)
                on { this.appData }.thenReturn(emptyList())
            }
            val event = mock<TransferEvent.TransferFinishEvent> {
                on { this.transfer } doReturn transfer
            }
            val eventError = mock<TransferEvent.TransferFinishEvent> {
                on { this.transfer } doReturn transfer
                on { this.error }.thenReturn(BusinessAccountExpiredMegaException(1))
            }
            add(event)
            add(eventError)
        }
    }
}