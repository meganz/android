package mega.privacy.android.domain.usecase.transfers.active

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.ActiveTransfer
import mega.privacy.android.domain.repository.TransferRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
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
    private val correctActiveTransfersUseCase = mock<CorrectActiveTransfersUseCase>()
    private val mockedActiveTransfers = (0..10).map { mock<ActiveTransfer>() }

    @BeforeAll
    fun setUp() {
        underTest = ClearActiveTransfersIfFinishedUseCase(
            transferRepository = transferRepository,
            correctActiveTransfersUseCase = correctActiveTransfersUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            transferRepository,
            correctActiveTransfersUseCase,
            *mockedActiveTransfers.toTypedArray()
        )
    }

    private fun stubActiveTransfers(allFinished: Boolean) = runTest {
        mockedActiveTransfers.forEachIndexed { index, activeTransfer ->
            whenever(activeTransfer.isFinished).thenReturn(allFinished || index != 0)
        }
        whenever(transferRepository.getCurrentActiveTransfers()).thenReturn(
            mockedActiveTransfers
        )
    }

    private fun stubEmptyActiveTransfers() = runTest {
        whenever(transferRepository.getCurrentActiveTransfers()).thenReturn(
            emptyList()
        )
    }

    @Test
    fun `test that correctActiveTransfersUseCase is invoked when this is invoked`() = runTest {
        stubEmptyActiveTransfers()
        underTest()
        verify(correctActiveTransfersUseCase).invoke(null)
    }

    @Test
    fun `test that transferRepository deleteAllActiveTransfers is invoked if all transfers are finished`() =
        runTest {
            stubActiveTransfers(true)

            underTest()
            verify(transferRepository).deleteAllActiveTransfers()
        }

    @Test
    fun `test that transferRepository deleteAllActiveTransfers is not invoked if not all transfers are finished`() =
        runTest {
            stubActiveTransfers(false)
            underTest()
            verify(transferRepository, never()).deleteAllActiveTransfers()
        }

    @Test
    fun `test that transferRepository deleteAllActiveTransfers is not invoked if active transfers are empty`() =
        runTest {
            stubEmptyActiveTransfers()
            underTest()
            verify(transferRepository, never()).deleteAllActiveTransfers()
        }
}