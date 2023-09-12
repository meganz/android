package mega.privacy.android.domain.usecase.transfers.active

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.ActiveTransfer
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.transfers.GetInProgressTransfersUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CleanActiveTransfersUseCaseTest {
    private lateinit var underTest: CleanActiveTransfersUseCase

    private val transferRepository = mock<TransferRepository>()
    private val getInProgressTransfersUseCase = mock<GetInProgressTransfersUseCase>()
    private val mockedActiveTransfers = (0..10).map { mock<ActiveTransfer>() }
    private val mockedCurrentTransfers = (0..5).map { mock<Transfer>() }

    @BeforeAll
    fun setUp() {
        underTest = CleanActiveTransfersUseCase(
            getInProgressTransfersUseCase = getInProgressTransfersUseCase,
            transferRepository = transferRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            transferRepository,
            getInProgressTransfersUseCase,
            *mockedActiveTransfers.toTypedArray()
        )
    }

    private fun stubActiveTransfers(areFinished: Boolean) {
        mockedActiveTransfers.forEachIndexed { index, activeTransfer ->
            whenever(activeTransfer.tag).thenReturn(index)
            whenever(activeTransfer.isFinished).thenReturn(areFinished)
        }
    }

    private fun stubCurrentTransfers() {
        mockedCurrentTransfers.forEachIndexed { index, transfer ->
            whenever(transfer.tag).thenReturn(index * 2)
        }
    }

    @Test
    fun `test that active transfers not finished and not in progress are removed`() = runTest {
        stubActiveTransfers(false)
        stubCurrentTransfers()
        whenever(transferRepository.getCurrentActiveTransfersByType(any()))
            .thenReturn(mockedActiveTransfers)
        whenever(getInProgressTransfersUseCase()).thenReturn(mockedCurrentTransfers)
        val expected =
            mockedActiveTransfers.map { it.tag } - mockedCurrentTransfers.map { it.tag }.toSet()
        Truth.assertThat(expected).isNotEmpty()
        underTest(TransferType.TYPE_UPLOAD)
        verify(transferRepository).deleteActiveTransferByTag(expected)
    }

    @Test
    fun `test that active transfers finished and not in progress are not removed`() = runTest {
        stubActiveTransfers(true)
        stubCurrentTransfers()
        whenever(transferRepository.getCurrentActiveTransfersByType(any()))
            .thenReturn(mockedActiveTransfers)
        whenever(getInProgressTransfersUseCase()).thenReturn(mockedCurrentTransfers)
        underTest(TransferType.TYPE_UPLOAD)
        verify(transferRepository).deleteActiveTransferByTag(emptyList())
    }
}