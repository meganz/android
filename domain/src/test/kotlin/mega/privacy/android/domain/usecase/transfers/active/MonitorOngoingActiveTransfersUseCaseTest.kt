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
import mega.privacy.android.domain.usecase.transfers.overquota.MonitorTransferOverQuotaUseCase
import mega.privacy.android.domain.usecase.transfers.paused.MonitorDownloadTransfersPausedUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorOngoingActiveTransfersUseCaseTest {
    private lateinit var underTest: MonitorOngoingActiveTransfersUseCase

    private val monitorActiveTransferTotalsUseCase = mock<MonitorActiveTransferTotalsUseCase>()
    private val getActiveTransferTotalsUseCase = mock<GetActiveTransferTotalsUseCase>()
    private val monitorDownloadTransfersPausedUseCase =
        mock<MonitorDownloadTransfersPausedUseCase>()
    private val monitorTransferOverQuotaUseCase = mock<MonitorTransferOverQuotaUseCase>()


    @BeforeEach
    fun resetMocks() {
        reset(
            monitorActiveTransferTotalsUseCase,
            getActiveTransferTotalsUseCase,
            monitorDownloadTransfersPausedUseCase,
            monitorTransferOverQuotaUseCase,
        )
    }

    @BeforeAll
    fun setUp() {
        underTest = MonitorOngoingActiveTransfersUseCase(
            monitorActiveTransferTotalsUseCase = monitorActiveTransferTotalsUseCase,
            getActiveTransferTotalsUseCase = getActiveTransferTotalsUseCase,
            monitorDownloadTransfersPausedUseCase = monitorDownloadTransfersPausedUseCase,
            monitorTransferOverQuotaUseCase = monitorTransferOverQuotaUseCase,
        )
    }

    @ParameterizedTest
    @EnumSource(TransferType::class)
    fun `test that getActiveTransferTotalsUseCase is emitted as first result`(
        transferType: TransferType,
    ) = runTest {
        val activeTransferTotals = mockActiveTransfersTotals(true)
        whenever(monitorActiveTransferTotalsUseCase(transferType))
            .thenReturn(flowOf())
        whenever(monitorDownloadTransfersPausedUseCase())
            .thenReturn(flowOf(false))
        whenever(getActiveTransferTotalsUseCase(transferType))
            .thenReturn(activeTransferTotals)
        whenever(monitorTransferOverQuotaUseCase()).thenReturn(flowOf())
        underTest(transferType).test {
            assertThat(awaitItem())
                .isEqualTo(MonitorOngoingActiveTransfersResult(activeTransferTotals, false, false))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @ParameterizedTest
    @EnumSource(TransferType::class)
    fun `test that flow ends when there are no more ongoing active transfers`(
        transferType: TransferType,
    ) = runTest {
        val first = mockActiveTransfersTotals(true)
        val last = mockActiveTransfersTotals(false)
        whenever(monitorActiveTransferTotalsUseCase(transferType))
            .thenReturn(MutableStateFlow(last))
        whenever(monitorDownloadTransfersPausedUseCase())
            .thenReturn(flowOf(false))
        whenever(getActiveTransferTotalsUseCase(transferType))
            .thenReturn(first)
        whenever(monitorTransferOverQuotaUseCase()).thenReturn(flowOf())
        val lastReceived = underTest(transferType).last()
        assertThat(lastReceived).isEqualTo(
            MonitorOngoingActiveTransfersResult(
                last,
                paused = false,
                overQuota = false
            )
        )
    }

    @ParameterizedTest
    @EnumSource(TransferType::class)
    fun `test that monitorDownloadTransfersPausedUseCase values are emitted`(
        transferType: TransferType,
    ) = runTest {
        val activeTransferTotals = mockActiveTransfersTotals(true)
        whenever(monitorActiveTransferTotalsUseCase(transferType))
            .thenReturn(flowOf())
        val pausedFlow = MutableStateFlow(false)
        whenever(monitorDownloadTransfersPausedUseCase())
            .thenReturn(pausedFlow)
        whenever(getActiveTransferTotalsUseCase(transferType))
            .thenReturn(activeTransferTotals)
        whenever(monitorTransferOverQuotaUseCase()).thenReturn(flowOf())
        underTest(transferType).test {
            assertThat(awaitItem())
                .isEqualTo(
                    MonitorOngoingActiveTransfersResult(
                        activeTransferTotals,
                        paused = false,
                        overQuota = false
                    )
                )
            pausedFlow.emit(true)
            assertThat(awaitItem())
                .isEqualTo(
                    MonitorOngoingActiveTransfersResult(
                        activeTransferTotals,
                        paused = true,
                        overQuota = false
                    )
                )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @ParameterizedTest
    @EnumSource(TransferType::class)
    fun `test that monitorTransferOverQuotaUseCase values are emitted`(
        transferType: TransferType,
    ) = runTest {
        val activeTransferTotals = mockActiveTransfersTotals(true)
        whenever(monitorActiveTransferTotalsUseCase(transferType))
            .thenReturn(flowOf())
        val overQuotaFlow = MutableStateFlow(false)
        whenever(monitorTransferOverQuotaUseCase())
            .thenReturn(overQuotaFlow)
        whenever(getActiveTransferTotalsUseCase(transferType))
            .thenReturn(activeTransferTotals)
        whenever(monitorDownloadTransfersPausedUseCase()).thenReturn(flowOf(false))
        underTest(transferType).test {
            assertThat(awaitItem())
                .isEqualTo(
                    MonitorOngoingActiveTransfersResult(
                        activeTransferTotals,
                        paused = false,
                        overQuota = false
                    )
                )
            overQuotaFlow.emit(true)
            assertThat(awaitItem())
                .isEqualTo(
                    MonitorOngoingActiveTransfersResult(
                        activeTransferTotals,
                        paused = false,
                        overQuota = true
                    )
                )
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun mockActiveTransfersTotals(hasOngoingTransfers: Boolean) =
        mock<ActiveTransferTotals> {
            on { hasOngoingTransfers() }.thenReturn(hasOngoingTransfers)
        }
}