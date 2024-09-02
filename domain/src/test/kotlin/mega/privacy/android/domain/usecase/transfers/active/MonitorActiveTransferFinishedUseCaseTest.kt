package mega.privacy.android.domain.usecase.transfers.active

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.repository.TransferRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorActiveTransferFinishedUseCaseTest {
    private lateinit var underTest: MonitorActiveTransferFinishedUseCase

    private val transferRepository = mock<TransferRepository>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorActiveTransferFinishedUseCase(transferRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(transferRepository)
    }

    @ParameterizedTest
    @EnumSource(TransferType::class)
    fun `test that last non zero totalCompletedFileTransfers is emitted when active transfers totals with 0 totalCompletedFileTransfers is received`(
        type: TransferType,
    ) = runTest {
        val flow = createTotals(5, 6, 7, 0)
        whenever(transferRepository.getActiveTransferTotalsByType(any())) doReturn flow.asFlow()

        underTest(type).test {
            assertThat(awaitItem()).isEqualTo(7)
            awaitComplete()
        }
    }

    @ParameterizedTest
    @EnumSource(TransferType::class)
    fun `test that last non zero totalCompletedFileTransfers are emitted for multiple active transfers totals with 0 totalCompletedFileTransfers is received`(
        type: TransferType,
    ) = runTest {
        val flow = createTotals(5, 0, 6, 0, 0, 7, 0)
        whenever(transferRepository.getActiveTransferTotalsByType(any())) doReturn flow.asFlow()

        underTest(type).test {
            assertThat(awaitItem()).isEqualTo(5)
            assertThat(awaitItem()).isEqualTo(6)
            assertThat(awaitItem()).isEqualTo(7)
            awaitComplete()
        }
    }

    @ParameterizedTest
    @EnumSource(TransferType::class)
    fun `test that no value is emitted when active transfers totals with 0 totalCompletedFileTransfers is not received`(
        type: TransferType,
    ) = runTest {
        val flow = createTotals(5, 6, 7)
        whenever(transferRepository.getActiveTransferTotalsByType(any())) doReturn flow.asFlow()

        underTest(type).test {
            awaitComplete()
        }
    }

    @ParameterizedTest
    @EnumSource(TransferType::class)
    fun `test that no value is emitted when first active transfers totals has 0 totalCompletedFileTransfers`(
        type: TransferType,
    ) = runTest {
        val flow = createTotals(0, 5, 6, 7)
        whenever(transferRepository.getActiveTransferTotalsByType(any())) doReturn flow.asFlow()

        underTest(type).test {
            awaitComplete()
        }
    }

    @ParameterizedTest
    @EnumSource(TransferType::class)
    fun `test that no value is emitted when all transfers were already downloaded`(
        type: TransferType,
    ) = runTest {
        val flow = listOf(
            mockTotal(0, 5),
            mockTotal(0)
        )
        whenever(transferRepository.getActiveTransferTotalsByType(any())) doReturn flow.asFlow()

        underTest(type).test {
            awaitComplete()
        }
    }

    @ParameterizedTest
    @EnumSource(TransferType::class)
    fun `test that totalCompletedFileTransfers is emitted when there's a mix of completed and already download files `(
        type: TransferType,
    ) = runTest {
        val flow = listOf(
            mockTotal(1, 5),
            mockTotal(0, 5)
        )
        whenever(transferRepository.getActiveTransferTotalsByType(any())) doReturn flow.asFlow()

        underTest(type).test {
            assertThat(awaitItem()).isEqualTo(1)
            awaitComplete()
        }
    }

    private fun createTotals(vararg totals: Int) = totals.map { mockTotal(it) }

    private fun mockTotal(
        totalCompleted: Int,
        totalAlreadyDownloadedFiles: Int = 0,
    ) = mock<ActiveTransferTotals> {
        on { this.totalCompletedFileTransfers } doReturn totalCompleted
        on { this.totalAlreadyTransferredFiles } doReturn totalAlreadyDownloadedFiles
    }

}