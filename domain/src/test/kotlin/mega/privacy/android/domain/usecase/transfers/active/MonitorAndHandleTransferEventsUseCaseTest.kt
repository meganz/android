package mega.privacy.android.domain.usecase.transfers.active

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.transfers.MonitorTransferEventsUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.time.Duration.Companion.milliseconds


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorAndHandleTransferEventsUseCaseTest {

    private lateinit var underTest: MonitorAndHandleTransferEventsUseCase

    private val monitorTransferEventsUseCase = mock<MonitorTransferEventsUseCase>()
    private val handleTransferEventsUseCases =
        setOf(mock<HandleTransferEventUseCase>(), mock<HandleTransferEventUseCase>())
    private val transferRepository = mock<TransferRepository>()

    @BeforeAll
    fun setup() {
        underTest = MonitorAndHandleTransferEventsUseCase(
            monitorTransferEventsUseCase,
            handleTransferEventsUseCases,
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

    @Test
    fun `test that events are handled in chunks`() = runTest {
        val fileEvent1 = TransferEvent.TransferUpdateEvent(mock())
        val fileEvent2 = TransferEvent.TransferUpdateEvent(mock())
        val fileEvent3 = TransferEvent.TransferUpdateEvent(mock())
        val fileEvent4 = TransferEvent.TransferUpdateEvent(mock())
        val fileEvent5 = TransferEvent.TransferUpdateEvent(mock())
        whenever(monitorTransferEventsUseCase()).thenReturn(
            flow {
                emit(fileEvent1)
                emit(fileEvent2)
                delay(200.milliseconds)
                emit(fileEvent3)
                emit(fileEvent4)
                emit(fileEvent5)
            }
        )

        underTest(100.milliseconds).test {
            assertThat(awaitItem()).isEqualTo(2)
            assertThat(awaitItem()).isEqualTo(3)
            awaitComplete()
        }
        handleTransferEventsUseCases.forEach {
            verify(it).invoke(fileEvent1, fileEvent2)
            verify(it).invoke(fileEvent3, fileEvent4, fileEvent5)
        }
    }

    @ParameterizedTest
    @MethodSource("provideNotRelevantTransfers")
    fun `test that not relevant events are filtered out`(
        notRelevantTransfer: Transfer,
    ) = runTest {
        val transferEvent = TransferEvent.TransferUpdateEvent(mock())
        whenever(monitorTransferEventsUseCase()).thenReturn(
            flowOf(
                transferEvent,
                TransferEvent.TransferUpdateEvent(notRelevantTransfer)
            )
        )

        underTest().test {
            assertThat(awaitItem()).isEqualTo(1)
            awaitComplete()
        }
        handleTransferEventsUseCases.forEach {
            verify(it).invoke(transferEvent)
        }
    }

    @Nested
    inner class WorkerStartTests {
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
        fun `test that download worker is started only once when is not running and multiple download events are received`(
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
        fun `test that download worker is not restarted again when it finishes and last event was a download event`(
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
                assertThat(cancelAndConsumeRemainingEvents()).isEmpty()
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
                awaitItem()
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

    private fun provideNotRelevantTransfers() = listOf<Transfer>(
        mock {
            on { appData } doReturn listOf(TransferAppData.VoiceClip)
        },
        mock {
            on { appData } doReturn listOf(TransferAppData.BackgroundTransfer)
        },
        mock {
            on { isStreamingTransfer } doReturn true
        },
        mock {
            on { isBackupTransfer } doReturn true
        },
        mock {
            on { isSyncTransfer } doReturn true
        },
        mock {
            on { transferType } doReturn TransferType.CU_UPLOAD
        }
    )
}