package mega.privacy.android.domain.usecase.transfers.paused

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.repository.TransferRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorAllTransfersPausedByTypeUseCaseTest {
    private lateinit var underTest: MonitorAllTransfersPausedByTypeUseCase

    private val transferRepository = mock<TransferRepository>()
    private val transfer = mock<Transfer>()

    @BeforeAll
    fun setup() {
        underTest = MonitorAllTransfersPausedByTypeUseCase(
            transferRepository
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(transferRepository, transfer)
    }

    @ParameterizedTest(name = "Global transfers paused {0}")
    @ValueSource(booleans = [true, false])
    fun `test that emits correct value when monitorPausedTransfers emits and there are no paused transfers`(
        paused: Boolean,
    ) = runTest {
        val type = TransferType.DOWNLOAD
        val pausedFlow = MutableStateFlow(paused)
        whenever(transferRepository.monitorPausedTransfers()).thenReturn(pausedFlow)
        whenever(transferRepository.monitorTransferEvents()).thenReturn(emptyFlow())
        val activeTransferTotals = mock<ActiveTransferTotals> {
            whenever(it.allPaused()) doReturn false
        }
        whenever(transferRepository.getActiveTransferTotalsByType(type)) doReturn
                flowOf(activeTransferTotals)
        underTest(type).test {
            assertThat(awaitItem()).isEqualTo(paused)
        }
    }

    @ParameterizedTest(name = "All individual transfers paused: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that emits correct value when monitorPausedTransfers is false and there are pending transfers`(
        paused: Boolean,
    ) = runTest {
        val type = TransferType.DOWNLOAD
        val pausedFlow = MutableStateFlow(false)
        whenever(transferRepository.monitorPausedTransfers()).thenReturn(pausedFlow)
        whenever(transferRepository.monitorTransferEvents()).thenReturn(emptyFlow())
        val activeTransferTotals = mock<ActiveTransferTotals> {
            whenever(it.allPaused()) doReturn paused
        }
        whenever(transferRepository.getActiveTransferTotalsByType(type)) doReturn
                flowOf(activeTransferTotals)
        underTest(type).test {
            assertThat(awaitItem()).isEqualTo(paused)
        }
    }

    @ParameterizedTest
    @EnumSource(TransferType::class, names = ["CU_UPLOAD", "NONE"], mode = EnumSource.Mode.EXCLUDE)
    fun `test that emits false when monitorPausedTransfers is false and there are no pending transfers`(
        type: TransferType,
    ) = runTest {
        val pausedFlow = MutableStateFlow(false)
        whenever(transferRepository.monitorPausedTransfers()).thenReturn(pausedFlow)
        whenever(transferRepository.monitorTransferEvents()).thenReturn(emptyFlow())
        val activeTransferTotals = mock<ActiveTransferTotals> {
            whenever(it.pausedFileTransfers) doReturn 0
            whenever(it.pendingFileTransfers) doReturn 0
        }
        whenever(transferRepository.getActiveTransferTotalsByType(type)) doReturn
                flowOf(activeTransferTotals)
        underTest(type).test {
            assertThat(awaitItem()).isEqualTo(false)
        }
    }

    @Test
    fun `test that repository specific camera uploads methods are used when type is camera upload`() =
        runTest {
            val type = TransferType.CU_UPLOAD
            val pausedFlow = MutableStateFlow(false)
            whenever(transferRepository.monitorPausedTransfers()).thenReturn(pausedFlow)
            whenever(transferRepository.monitorTransferEvents()).thenReturn(emptyFlow())
            whenever(transferRepository.getNumPendingCameraUploads()).thenReturn(0)
            whenever(transferRepository.getNumPendingPausedCameraUploads()).thenReturn(0)

            underTest(type).test {
                assertThat(awaitItem()).isEqualTo(false)
            }
            verify(transferRepository, never()).getActiveTransferTotalsByType(any())
        }

    @ParameterizedTest
    @EnumSource(TransferType::class, names = ["CU_UPLOAD", "NONE"], mode = EnumSource.Mode.EXCLUDE)
    fun `test that all individual transfers paused is emited when general transfers are not paused`(
        type: TransferType,
    ) = runTest {
        val pausedFlow = MutableStateFlow(false)
        val activeTransferTotalsFlow = MutableStateFlow(mock<ActiveTransferTotals> {
            whenever(it.allPaused()) doReturn false
        })
        whenever(transferRepository.monitorPausedTransfers()).thenReturn(pausedFlow)
        whenever(transferRepository.getActiveTransferTotalsByType(type)).thenReturn(
            activeTransferTotalsFlow
        )
        underTest(type).test {
            assertThat(awaitItem()).isEqualTo(false)
            activeTransferTotalsFlow.emit(mock<ActiveTransferTotals> {
                whenever(it.allPaused()) doReturn true
            })
            assertThat(awaitItem()).isEqualTo(true)
        }
    }
}