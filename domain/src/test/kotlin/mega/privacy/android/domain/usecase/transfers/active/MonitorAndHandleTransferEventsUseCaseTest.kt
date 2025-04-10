package mega.privacy.android.domain.usecase.transfers.active

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferAppData
import mega.privacy.android.domain.entity.transfer.TransferEvent
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.usecase.transfers.MonitorTransferEventsUseCase
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
import org.mockito.kotlin.whenever
import kotlin.time.Duration.Companion.milliseconds


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorAndHandleTransferEventsUseCaseTest {

    private lateinit var underTest: MonitorAndHandleTransferEventsUseCase

    private val monitorTransferEventsUseCase = mock<MonitorTransferEventsUseCase>()
    private val handleTransferEventUseCase = mock<HandleTransferEventUseCase>()

    @BeforeAll
    fun setup() {
        underTest = MonitorAndHandleTransferEventsUseCase(
            monitorTransferEventsUseCase,
            handleTransferEventUseCase,
        )
    }

    @BeforeEach
    fun cleanUp() {
        reset(
            monitorTransferEventsUseCase,
            handleTransferEventUseCase,
        )
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

        verify(handleTransferEventUseCase).invoke(fileEvent1, fileEvent2)
        verify(handleTransferEventUseCase).invoke(fileEvent3, fileEvent4, fileEvent5)
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

        verify(handleTransferEventUseCase).invoke(transferEvent)
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