package mega.privacy.android.domain.usecase.transfers

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.MonitorOngoingActiveTransfersResult
import mega.privacy.android.domain.entity.transfer.TransferProgressResult
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.usecase.transfers.active.MonitorOngoingActiveTransfersUseCase
import mega.privacy.android.domain.usecase.transfers.pending.GetPendingTransfersByTypeUseCase
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
class MonitorActiveAndPendingTransfersUseCaseTest {
    private lateinit var underTest: MonitorActiveAndPendingTransfersUseCase

    private val monitorOngoingActiveTransfersUseCase = mock<MonitorOngoingActiveTransfersUseCase>()
    private val getPendingTransfersByTypeUseCase = mock<GetPendingTransfersByTypeUseCase>()


    @BeforeAll
    fun setup() {
        underTest = MonitorActiveAndPendingTransfersUseCase(
            monitorOngoingActiveTransfersUseCase,
            getPendingTransfersByTypeUseCase,
        )
    }

    @BeforeEach
    fun cleanup() {
        reset(
            monitorOngoingActiveTransfersUseCase,
            getPendingTransfersByTypeUseCase,
        )
    }

    @ParameterizedTest
    @EnumSource(TransferType::class)
    fun `test that ongoing active transfers are emitted`(transferType: TransferType) =
        runTest {
            val expected = TransferProgressResult(
                mockMonitorOngoingActiveTransfersResult(true),
                pendingTransfers = false,
                ongoingTransfers = true,
            )
            whenever(monitorOngoingActiveTransfersUseCase(transferType)) doReturn
                    monitorOngoingActiveTransfersFlow(expected.monitorOngoingActiveTransfersResult)
            whenever(getPendingTransfersByTypeUseCase(transferType)) doReturn flowOf(emptyList())

            underTest(transferType).test {
                assertThat(awaitItem()).isEqualTo(expected)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @ParameterizedTest
    @EnumSource(TransferType::class)
    fun `test that a new value is emitted with pending work set to true when there are ongoing transfers`(
        transferType: TransferType,
    ) =
        runTest {
            val expected = true
            val monitorOngoingActiveTransfersResult = mockMonitorOngoingActiveTransfersResult(true)
            whenever(monitorOngoingActiveTransfersUseCase(transferType)) doReturn
                    monitorOngoingActiveTransfersFlow(monitorOngoingActiveTransfersResult)
            whenever(getPendingTransfersByTypeUseCase(transferType)) doReturn flowOf(emptyList())

            underTest(transferType).test {
                assertThat(awaitItem().pendingWork).isEqualTo(expected)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @ParameterizedTest
    @EnumSource(TransferType::class)
    fun `test that a new value is emitted with pending work set to false when there are no ongoing transfers`(
        transferType: TransferType,
    ) =
        runTest {
            val expected = false
            val monitorOngoingActiveTransfersResult = mockMonitorOngoingActiveTransfersResult(false)
            whenever(monitorOngoingActiveTransfersUseCase(transferType)) doReturn
                    monitorOngoingActiveTransfersFlow(monitorOngoingActiveTransfersResult)
            whenever(getPendingTransfersByTypeUseCase(transferType)) doReturn flowOf(emptyList())

            underTest(transferType).test {
                assertThat(awaitItem().pendingWork).isEqualTo(expected)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @ParameterizedTest
    @EnumSource(TransferType::class)
    fun `test that a new value is emitted with pending work set to true when there are no ongoing transfers but there are pending transfers`(
        transferType: TransferType,
    ) =
        runTest {
            val expected = true
            val monitorOngoingActiveTransfersResult = mockMonitorOngoingActiveTransfersResult(false)
            whenever(monitorOngoingActiveTransfersUseCase(transferType)) doReturn
                    monitorOngoingActiveTransfersFlow(monitorOngoingActiveTransfersResult)
            whenever(getPendingTransfersByTypeUseCase(transferType)) doReturn flowOf(listOf(mock()))

            underTest(transferType).test {
                assertThat(awaitItem().pendingWork).isEqualTo(expected)
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun monitorOngoingActiveTransfersFlow(monitorOngoingActiveTransfersResult: MonitorOngoingActiveTransfersResult): Flow<MonitorOngoingActiveTransfersResult> {
        return flow {
            emit(monitorOngoingActiveTransfersResult)
            awaitCancellation()
        }
    }

    private fun mockMonitorOngoingActiveTransfersResult(hasOngoingTransfers: Boolean): MonitorOngoingActiveTransfersResult {
        val activeTransferTotals = mock<ActiveTransferTotals> {
            on { this.hasOngoingTransfers() } doReturn hasOngoingTransfers
        }
        return MonitorOngoingActiveTransfersResult(
            activeTransferTotals,
            paused = false,
            transfersOverQuota = false,
            storageOverQuota = false
        )
    }
}