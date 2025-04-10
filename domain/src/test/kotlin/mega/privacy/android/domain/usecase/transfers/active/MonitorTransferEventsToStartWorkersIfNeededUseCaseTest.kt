package mega.privacy.android.domain.usecase.transfers.active

import app.cash.turbine.test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.transfers.MonitorTransferEventsUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorTransferEventsToStartWorkersIfNeededUseCaseTest {

    private lateinit var underTest: MonitorTransferEventsToStartWorkersIfNeededUseCase

    private val monitorTransferEventsUseCase = mock<MonitorTransferEventsUseCase>()
    private val handleTransferEventsUseCases =
        setOf(mock<HandleTransferEventUseCase>(), mock<HandleTransferEventUseCase>())
    private val transferRepository = mock<TransferRepository>()

    @BeforeAll
    fun setup() {
        underTest = MonitorTransferEventsToStartWorkersIfNeededUseCase(
            monitorTransferEventsUseCase,
            transferRepository,
        )
    }

    @BeforeEach
    fun cleanUp() {
        reset(
            monitorTransferEventsUseCase,
            transferRepository,
            *handleTransferEventsUseCases.toTypedArray(),
        )
        whenever(transferRepository.monitorIsDownloadsWorkerFinished()) doReturn flowOf(false)
        whenever(transferRepository.monitorIsUploadsWorkerFinished()) doReturn flowOf(false)
        whenever(transferRepository.monitorIsChatUploadsWorkerFinished()) doReturn flowOf(false)
    }

    @ParameterizedTest
    @EnumSource(TransferType::class, names = ["DOWNLOAD", "GENERAL_UPLOAD", "CHAT_UPLOAD"])
    fun `test that worker is started when is not running and an event of related type is received`(
        type: TransferType,
    ) = runTest {
        val transferEvent = TransferEvent.TransferUpdateEvent(
            mock<Transfer> {
                on { transferType } doReturn type
            }
        )
        whenever(monitorIsWorkerFinished(type)) doReturn flowOf(true)
        whenever(monitorTransferEventsUseCase()).thenReturn(
            flowOf(transferEvent)
        )
        underTest().test {
            awaitItem()
            awaitComplete()
        }
        verify(type)
    }

    @ParameterizedTest
    @EnumSource(TransferType::class, names = ["DOWNLOAD", "GENERAL_UPLOAD", "CHAT_UPLOAD"])
    fun `test that download worker is started only once when is not running and multiple events are received`(
        type: TransferType,
    ) = runTest {
        val transferEvents = (0..5).map {
            TransferEvent.TransferUpdateEvent(
                mock<Transfer> {
                    on { transferType } doReturn type
                }
            )
        }
        whenever(monitorIsWorkerFinished(type)) doReturn flowOf(true)
        whenever(monitorTransferEventsUseCase()).thenReturn(
            transferEvents.asFlow()
        )
        underTest().test {
            awaitItem()
            awaitComplete()
        }
        verify(type)
    }

    @ParameterizedTest
    @EnumSource(TransferType::class, names = ["DOWNLOAD", "GENERAL_UPLOAD", "CHAT_UPLOAD"])
    fun `test that download worker is not restarted again when it finishes and last event was an event of same type`(
        type: TransferType,
    ) = runTest {
        val transferEvent = TransferEvent.TransferUpdateEvent(
            mock<Transfer> {
                on { transferType } doReturn type
            }
        )
        val monitorIsDownloadsWorkerFinished = MutableStateFlow(true)
        whenever(monitorIsWorkerFinished(type)) doReturn monitorIsDownloadsWorkerFinished
        whenever(monitorTransferEventsUseCase()).thenReturn(
            flowOf(transferEvent)
        )
        underTest().test {
            awaitItem()
            monitorIsDownloadsWorkerFinished.emit(false) //simulate worker has been started
            monitorIsDownloadsWorkerFinished.emit(true) //simulate worker has finished
            //assertThat(cancelAndConsumeRemainingEvents()).isEmpty()
        }
        verify(type)
    }

    @ParameterizedTest
    @EnumSource(TransferType::class, names = ["DOWNLOAD", "GENERAL_UPLOAD", "CHAT_UPLOAD"])
    fun `test that download worker is not started when a download event is received but it is already running`(
        type: TransferType,
    ) = runTest {
        val transferEvent = TransferEvent.TransferUpdateEvent(
            mock<Transfer> {
                on { transferType } doReturn type
            }
        )
        whenever(monitorIsWorkerFinished(type)) doReturn flowOf(false)
        whenever(monitorTransferEventsUseCase()).thenReturn(
            flowOf(transferEvent)
        )
        underTest().test {
            awaitComplete()
        }
        verify(type, 0)
    }

    private fun monitorIsWorkerFinished(transferType: TransferType) = when (transferType) {
        TransferType.DOWNLOAD -> transferRepository.monitorIsDownloadsWorkerFinished()
        TransferType.GENERAL_UPLOAD -> transferRepository.monitorIsUploadsWorkerFinished()
        TransferType.CHAT_UPLOAD -> transferRepository.monitorIsChatUploadsWorkerFinished()
        else -> throw IllegalArgumentException("Invalid transfer type")
    }

    private suspend fun verify(type: TransferType, times: Int = 1) = when (type) {
        TransferType.DOWNLOAD ->
            verify(transferRepository, times(times)).startDownloadWorker()

        TransferType.GENERAL_UPLOAD ->
            verify(transferRepository, times(times)).startUploadsWorker()

        TransferType.CHAT_UPLOAD ->
            verify(transferRepository, times(times)).startChatUploadsWorker()

        else -> throw IllegalArgumentException("Invalid transfer type")
    }
}