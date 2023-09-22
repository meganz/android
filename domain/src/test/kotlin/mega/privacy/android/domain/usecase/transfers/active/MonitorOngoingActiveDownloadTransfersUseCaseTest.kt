package mega.privacy.android.domain.usecase.transfers.active

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.MonitorOngoingActiveTransfersResult
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.usecase.transfers.paused.MonitorDownloadTransfersPausedUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorOngoingActiveDownloadTransfersUseCaseTest {
    private lateinit var underTest: MonitorOngoingActiveDownloadTransfersUseCase

    private val monitorActiveTransferTotalsUseCase = mock<MonitorActiveTransferTotalsUseCase>()
    private val getActiveTransferTotalsUseCase = mock<GetActiveTransferTotalsUseCase>()
    private val monitorDownloadTransfersPausedUseCase =
        mock<MonitorDownloadTransfersPausedUseCase>()


    @BeforeEach
    fun resetMocks() {
        reset(
            monitorActiveTransferTotalsUseCase,
            getActiveTransferTotalsUseCase,
            monitorDownloadTransfersPausedUseCase,
        )
    }

    @BeforeAll
    fun setUp() {
        underTest = MonitorOngoingActiveDownloadTransfersUseCase(
            monitorActiveTransferTotalsUseCase = monitorActiveTransferTotalsUseCase,
            getActiveTransferTotalsUseCase = getActiveTransferTotalsUseCase,
            monitorDownloadTransfersPausedUseCase = monitorDownloadTransfersPausedUseCase,
        )
    }

    @Test
    fun `test that getActiveTransferTotalsUseCase is emitted as first result`() = runTest {
        val activeTransferTotals = mockActiveTransfersTotals(true)
        whenever(monitorActiveTransferTotalsUseCase(TransferType.DOWNLOAD))
            .thenReturn(flowOf())
        whenever(monitorDownloadTransfersPausedUseCase())
            .thenReturn(flowOf(false))
        whenever(getActiveTransferTotalsUseCase(TransferType.DOWNLOAD))
            .thenReturn(activeTransferTotals)
        underTest().test {
            assertThat(awaitItem())
                .isEqualTo(MonitorOngoingActiveTransfersResult(activeTransferTotals, false))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that flow ends when there are no more ongoing active transfers`() = runTest {
        val first = mockActiveTransfersTotals(true)
        val last = mockActiveTransfersTotals(false)
        whenever(monitorActiveTransferTotalsUseCase(TransferType.DOWNLOAD))
            .thenReturn(MutableStateFlow(last))
        whenever(monitorDownloadTransfersPausedUseCase())
            .thenReturn(flowOf(false))
        whenever(getActiveTransferTotalsUseCase(TransferType.DOWNLOAD))
            .thenReturn(first)
        val lastReceived = underTest(TEST_SAMPLE).last()
        assertThat(lastReceived).isEqualTo(MonitorOngoingActiveTransfersResult(last, false))
    }

    @Test
    fun `test that monitorDownloadTransfersPausedUseCase values are emitted`() = runTest {
        val activeTransferTotals = mockActiveTransfersTotals(true)
        whenever(monitorActiveTransferTotalsUseCase(TransferType.DOWNLOAD))
            .thenReturn(flowOf())
        val pausedFlow = MutableStateFlow(false)
        whenever(monitorDownloadTransfersPausedUseCase())
            .thenReturn(pausedFlow)
        whenever(getActiveTransferTotalsUseCase(TransferType.DOWNLOAD))
            .thenReturn(activeTransferTotals)
        underTest().test {
            assertThat(awaitItem())
                .isEqualTo(MonitorOngoingActiveTransfersResult(activeTransferTotals, false))
            pausedFlow.emit(true)
            assertThat(awaitItem())
                .isEqualTo(MonitorOngoingActiveTransfersResult(activeTransferTotals, true))
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun mockActiveTransfersTotals(hasOngoingTransfers: Boolean) =
        mock<ActiveTransferTotals> {
            on { hasOngoingTransfers() }.thenReturn(hasOngoingTransfers)
        }

    companion object {
        private const val TEST_SAMPLE = 50L
    }
}