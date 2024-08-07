package mega.privacy.android.domain.usecase.transfers.active

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.MonitorOngoingActiveTransfersResult
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.usecase.transfers.overquota.MonitorStorageOverQuotaUseCase
import mega.privacy.android.domain.usecase.transfers.overquota.MonitorTransferOverQuotaUseCase
import mega.privacy.android.domain.usecase.transfers.paused.MonitorAllTransfersPausedByTypeUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorOngoingActiveTransfersUseCaseTest {
    private lateinit var underTest: MonitorOngoingActiveTransfersUseCase

    private val monitorActiveTransferTotalsUseCase = mock<MonitorActiveTransferTotalsUseCase>()
    private val getActiveTransferTotalsUseCase = mock<GetActiveTransferTotalsUseCase>()
    private val monitorAllTransfersPausedByTypeUseCase =
        mock<MonitorAllTransfersPausedByTypeUseCase>()
    private val monitorTransferOverQuotaUseCase = mock<MonitorTransferOverQuotaUseCase>()
    private val monitorStorageOverQuotaUseCase = mock<MonitorStorageOverQuotaUseCase>()


    @BeforeAll
    fun setUp() {
        underTest = MonitorOngoingActiveTransfersUseCase(
            monitorActiveTransferTotalsUseCase = monitorActiveTransferTotalsUseCase,
            getActiveTransferTotalsUseCase = getActiveTransferTotalsUseCase,
            monitorAllTransfersPausedByTypeUseCase = monitorAllTransfersPausedByTypeUseCase,
            monitorTransferOverQuotaUseCase = monitorTransferOverQuotaUseCase,
            monitorStorageOverQuotaUseCase = monitorStorageOverQuotaUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            monitorActiveTransferTotalsUseCase,
            getActiveTransferTotalsUseCase,
            monitorAllTransfersPausedByTypeUseCase,
            monitorTransferOverQuotaUseCase,
            monitorStorageOverQuotaUseCase,
        )
        commonStub()
    }

    private fun commonStub() {
        whenever(monitorTransferOverQuotaUseCase()) doReturn flowOf(false)
        whenever(monitorStorageOverQuotaUseCase()) doReturn flowOf(false)
    }

    @ParameterizedTest
    @EnumSource(TransferType::class)
    fun `test that getActiveTransferTotalsUseCase is emitted as first result`(
        transferType: TransferType,
    ) = runTest {
        val activeTransferTotals = mockActiveTransfersTotals(true)
        whenever(monitorActiveTransferTotalsUseCase(transferType))
            .thenReturn(flowOf())
        whenever(monitorAllTransfersPausedByTypeUseCase(transferType))
            .thenReturn(flowOf(false))
        whenever(getActiveTransferTotalsUseCase(transferType))
            .thenReturn(activeTransferTotals)
        underTest(transferType).test {
            assertThat(awaitItem())
                .isEqualTo(
                    MonitorOngoingActiveTransfersResult(
                        activeTransferTotals,
                        false,
                        false,
                        false
                    )
                )
            cancelAndIgnoreRemainingEvents()
        }
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
        whenever(monitorAllTransfersPausedByTypeUseCase(transferType))
            .thenReturn(pausedFlow)
        whenever(getActiveTransferTotalsUseCase(transferType))
            .thenReturn(activeTransferTotals)
        underTest(transferType).test {
            assertThat(awaitItem())
                .isEqualTo(
                    MonitorOngoingActiveTransfersResult(
                        activeTransferTotals,
                        paused = false,
                        transfersOverQuota = false,
                        storageOverQuota = false,
                    )
                )
            pausedFlow.emit(true)
            assertThat(awaitItem())
                .isEqualTo(
                    MonitorOngoingActiveTransfersResult(
                        activeTransferTotals,
                        paused = true,
                        transfersOverQuota = false,
                        storageOverQuota = false,
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
        whenever(monitorAllTransfersPausedByTypeUseCase(transferType)).thenReturn(flowOf(false))
        underTest(transferType).test {
            assertThat(awaitItem())
                .isEqualTo(
                    MonitorOngoingActiveTransfersResult(
                        activeTransferTotals,
                        paused = false,
                        transfersOverQuota = false,
                        storageOverQuota = false,
                    )
                )
            overQuotaFlow.emit(true)
            assertThat(awaitItem())
                .isEqualTo(
                    MonitorOngoingActiveTransfersResult(
                        activeTransferTotals,
                        paused = false,
                        transfersOverQuota = true,
                        storageOverQuota = false,
                    )
                )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @ParameterizedTest
    @EnumSource(TransferType::class)
    fun `test that monitorStorageOverQuotaUseCase values are emitted`(
        transferType: TransferType,
    ) = runTest {
        val activeTransferTotals = mockActiveTransfersTotals(true)
        whenever(monitorActiveTransferTotalsUseCase(transferType))
            .thenReturn(flowOf())
        val overQuotaFlow = MutableStateFlow(false)
        whenever(monitorStorageOverQuotaUseCase())
            .thenReturn(overQuotaFlow)
        whenever(getActiveTransferTotalsUseCase(transferType))
            .thenReturn(activeTransferTotals)
        whenever(monitorAllTransfersPausedByTypeUseCase(transferType)).thenReturn(flowOf(false))
        underTest(transferType).test {
            assertThat(awaitItem())
                .isEqualTo(
                    MonitorOngoingActiveTransfersResult(
                        activeTransferTotals,
                        paused = false,
                        transfersOverQuota = false,
                        storageOverQuota = false,
                    )
                )
            overQuotaFlow.emit(true)
            assertThat(awaitItem())
                .isEqualTo(
                    MonitorOngoingActiveTransfersResult(
                        activeTransferTotals,
                        paused = false,
                        transfersOverQuota = false,
                        storageOverQuota = true,
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