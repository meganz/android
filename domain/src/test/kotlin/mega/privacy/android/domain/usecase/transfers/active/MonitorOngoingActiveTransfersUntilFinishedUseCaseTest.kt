package mega.privacy.android.domain.usecase.transfers.active

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.MonitorOngoingActiveTransfersResult
import mega.privacy.android.domain.entity.transfer.TransferType
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorOngoingActiveTransfersUntilFinishedUseCaseTest {
    private lateinit var underTest: MonitorOngoingActiveTransfersUntilFinishedUseCase

    private val monitorOngoingActiveTransfersUseCase = mock<MonitorOngoingActiveTransfersUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorOngoingActiveTransfersUntilFinishedUseCase(
            monitorOngoingActiveTransfersUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            monitorOngoingActiveTransfersUseCase
        )
    }

    @ParameterizedTest
    @EnumSource(TransferType::class)
    fun `test that flow ends when there are no more ongoing active transfers`(
        transferType: TransferType,
    ) = runTest {
        val first = mockActiveTransfersTotals(true)
        val last = mockActiveTransfersTotals(false)
        whenever(monitorOngoingActiveTransfersUseCase(transferType)) doReturn flowOf(first, last)
        val lastReceived = underTest(transferType).last()
        assertThat(lastReceived).isEqualTo(
            MonitorOngoingActiveTransfersResult(
                last.activeTransferTotals,
                paused = false,
                transfersOverQuota = false,
                storageOverQuota = false,
            )
        )
    }

    private fun mockActiveTransfersTotals(hasOngoingTransfers: Boolean): MonitorOngoingActiveTransfersResult {
        val activeTransferTotals = mock<ActiveTransferTotals> {
            on { hasOngoingTransfers() }.thenReturn(hasOngoingTransfers)
        }
        return MonitorOngoingActiveTransfersResult(
            activeTransferTotals,
            paused = false,
            transfersOverQuota = false,
            storageOverQuota = false,
        )
    }
}