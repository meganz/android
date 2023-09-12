package mega.privacy.android.domain.usecase.transfers.active

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.ActiveTransfer
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.repository.TransferRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ClearActiveTransfersIfFinishedUseCaseTest {

    private lateinit var underTest: ClearActiveTransfersIfFinishedUseCase

    private val transferRepository = mock<TransferRepository>()
    private val cleanActiveTransfersUseCase = mock<CleanActiveTransfersUseCase>()
    private val mockedActiveTransfers = (0..10).map { mock<ActiveTransfer>() }

    @BeforeAll
    fun setUp() {
        underTest = ClearActiveTransfersIfFinishedUseCase(
            transferRepository = transferRepository,
            cleanActiveTransfersUseCase = cleanActiveTransfersUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            transferRepository,
            cleanActiveTransfersUseCase,
            *mockedActiveTransfers.toTypedArray()
        )
    }

    private fun stubActiveTransfers(allFinished: Boolean) = runTest {
        mockedActiveTransfers.forEachIndexed { index, activeTransfer ->
            whenever(activeTransfer.isFinished).thenReturn(allFinished || index != 0)
        }
        whenever(transferRepository.getCurrentActiveTransfersByType(any())).thenReturn(
            mockedActiveTransfers
        )
    }

    private fun stubEmptyActiveTransfers() = runTest {
        whenever(transferRepository.getCurrentActiveTransfersByType(any())).thenReturn(
            emptyList()
        )
    }

    @ParameterizedTest
    @EnumSource(TransferType::class)
    fun `test that cleanActiveTransfersUseCase is invoked when this is invoked`(
        transferType: TransferType,
    ) = runTest {
        stubEmptyActiveTransfers()
        underTest(transferType)
        verify(cleanActiveTransfersUseCase).invoke(transferType)
    }

    @ParameterizedTest
    @EnumSource(TransferType::class)
    fun `test that transferRepository deleteAllActiveTransfersByType is invoked if all transfers are finished`(
        transferType: TransferType,
    ) = runTest {
        stubActiveTransfers(true)

        underTest(transferType)
        verify(transferRepository).deleteAllActiveTransfersByType(transferType)
    }

    @ParameterizedTest
    @EnumSource(TransferType::class)
    fun `test that transferRepository deleteAllActiveTransfersByType is not invoked if not all transfers are finished`(
        transferType: TransferType,
    ) = runTest {
        stubActiveTransfers(false)
        underTest(transferType)
        verify(transferRepository, never()).deleteAllActiveTransfersByType(transferType)
    }

    @ParameterizedTest
    @EnumSource(TransferType::class)
    fun `test that transferRepository deleteAllActiveTransfersByType is not invoked if active transfers are empty`(
        transferType: TransferType,
    ) = runTest {
        stubEmptyActiveTransfers()
        underTest(transferType)
        verify(transferRepository, never()).deleteAllActiveTransfersByType(transferType)
    }
}