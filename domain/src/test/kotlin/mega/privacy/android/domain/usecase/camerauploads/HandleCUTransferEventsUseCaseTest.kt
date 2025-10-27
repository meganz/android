package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.repository.CameraUploadsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.KStubbing
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HandleCUTransferEventsUseCaseTest {

    private lateinit var underTest: HandleCUTransferEventsUseCase

    private val cameraUploadsRepository = mock<CameraUploadsRepository>()

    @BeforeAll
    fun setUp() {
        underTest = HandleCUTransferEventsUseCase(
            cameraUploadsRepository = cameraUploadsRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(cameraUploadsRepository)
    }

    @ParameterizedTest
    @MethodSource("provideStartPauseUpdateEvents")
    fun `test that updateCameraUploadsInProgressTransfers in repository is invoked when start, pause or update CU transfer event is received`(
        transferEvent: TransferEvent,
    ) = runTest {
        underTest(transferEvent)
        if (transferEvent.transfer.transferType == TransferType.CU_UPLOAD) {
            verify(cameraUploadsRepository).updateCameraUploadsInProgressTransfers(
                eq(listOf(transferEvent.transfer))
            )
        } else {
            verify(cameraUploadsRepository, never()).updateCameraUploadsInProgressTransfers(any())
        }
    }

    @Test
    fun `test that invoke calls updateCameraUploadsInProgressTransfers with the last event of each transfer when multiple CU transfer events are send`() =
        runTest {
            val events1 = listOf(
                mockTransferEvent<TransferEvent.TransferStartEvent>(TransferType.CU_UPLOAD, 1),
                mockTransferEvent<TransferEvent.TransferPaused>(TransferType.CU_UPLOAD, 1),
                mockTransferEvent<TransferEvent.TransferUpdateEvent>(TransferType.CU_UPLOAD, 1),
            )
            val events2 = listOf(
                mockTransferEvent<TransferEvent.TransferStartEvent>(TransferType.DOWNLOAD, 2),
                mockTransferEvent<TransferEvent.TransferPaused>(TransferType.DOWNLOAD, 2) {
                    on { paused } doReturn true
                },
                mockTransferEvent<TransferEvent.TransferPaused>(TransferType.DOWNLOAD, 2) {
                    on { paused } doReturn false
                },
            )
            val events3 = listOf(
                mockTransferEvent<TransferEvent.TransferUpdateEvent>(TransferType.CU_UPLOAD, 3),
            )
            underTest.invoke(events = (events1 + events2 + events3).toTypedArray())
            verify(cameraUploadsRepository).updateCameraUploadsInProgressTransfers(
                eq(
                    listOf(
                        events1.last().transfer,
                        events3.last().transfer,
                    )
                )
            )
        }

    private fun provideStartPauseUpdateEvents() =
        provideTransferEvents<TransferEvent.TransferStartEvent>() +
                provideTransferEvents<TransferEvent.TransferPaused>() +
                provideTransferEvents<TransferEvent.TransferUpdateEvent>()

    private inline fun <reified T : TransferEvent> provideTransferEvents(
        transferUniqueId: Long = 0,
        isFinished: Boolean = false,
        stubbing: KStubbing<T>.(T) -> Unit = {},
    ) = TransferType.entries.map { transferType ->
        mockTransferEvent(
            transferType = transferType,
            transferUniqueId = transferUniqueId,
            isFinished = isFinished,
            stubbing = stubbing
        )
    }

    private inline fun <reified T : TransferEvent> mockTransferEvent(
        transferType: TransferType,
        transferUniqueId: Long = 0,
        isFinished: Boolean = false,
        folderTransferTag: Int? = null,
        isFolderTransfer: Boolean = false,
        stubbing: KStubbing<T>.(T) -> Unit = {},
    ): T {
        val transfer = mock<Transfer> {
            on { this.transferType }.thenReturn(transferType)
            on { this.uniqueId }.thenReturn(transferUniqueId)
            on { this.isFinished }.thenReturn(isFinished)
            on { this.folderTransferTag }.thenReturn(folderTransferTag)
            on { this.isFolderTransfer }.thenReturn(isFolderTransfer)
            on { this.appData }.thenReturn(emptyList())
        }
        return mock<T> {
            on { this.transfer }.thenReturn(transfer)
            stubbing(it)
        }
    }
}