package mega.privacy.android.domain.usecase.transfers.active

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.usecase.qrcode.ScanMediaFileUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HandleDownloadTransferEventsUseCaseTest {
    private lateinit var underTest: HandleDownloadTransferEventsUseCase

    private val scanMediaFileUseCase = mock<ScanMediaFileUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = HandleDownloadTransferEventsUseCase(scanMediaFileUseCase)
    }

    @BeforeEach
    fun cleanUp() {
        reset(scanMediaFileUseCase)
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
}