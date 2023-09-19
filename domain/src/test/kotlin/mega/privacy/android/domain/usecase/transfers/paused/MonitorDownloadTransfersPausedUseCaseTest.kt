package mega.privacy.android.domain.usecase.transfers.paused

import com.google.common.truth.Truth.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.repository.TransferRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorDownloadTransfersPausedUseCaseTest {
    private lateinit var underTest: MonitorDownloadTransfersPausedUseCase

    private val transferRepository = mock<TransferRepository>()

    @BeforeAll
    fun setup() {
        underTest = MonitorDownloadTransfersPausedUseCase(transferRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(transferRepository)
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 1, 5, 99])
    fun `test that totalPendingIndividualTransfers returns getCurrentActiveTransferTotalsByType pendingFileTransfers from repository`(
        value: Int,
    ) = runTest {
        val totals = mock<ActiveTransferTotals> {
            on { pendingFileTransfers }.thenReturn(value)
        }
        whenever(transferRepository.getCurrentActiveTransferTotalsByType(TransferType.TYPE_DOWNLOAD))
            .thenReturn(totals)
        assertThat(underTest.totalPendingIndividualTransfers()).isEqualTo(value)
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 1, 5, 99])
    fun `test that totalPausedIndividualTransfers returns getCurrentActiveTransferTotalsByType pausedFileTransfers from repository`(
        value: Int,
    ) = runTest {
        val totals = mock<ActiveTransferTotals> {
            on { pausedFileTransfers }.thenReturn(value)
        }
        whenever(transferRepository.getCurrentActiveTransferTotalsByType(TransferType.TYPE_DOWNLOAD))
            .thenReturn(totals)
        assertThat(underTest.totalPausedIndividualTransfers()).isEqualTo(value)
    }

    @ParameterizedTest
    @MethodSource("getTransfers")
    fun `test that transfers are filtered correctly`(transfer: Transfer, isCorrect: Boolean) {
        assertThat(underTest.isCorrectType(transfer)).isEqualTo(isCorrect)
    }

    private fun getTransfers() = listOf(
        Arguments.of(mockTransfer(TransferType.TYPE_DOWNLOAD), true),
        Arguments.of(mockTransfer(TransferType.TYPE_UPLOAD), false),
        Arguments.of(mockTransfer(TransferType.NONE), false),
    )

    private fun mockTransfer(type: TransferType) = mock<Transfer> {
        on { it.transferType }.thenReturn(type)
    }

}