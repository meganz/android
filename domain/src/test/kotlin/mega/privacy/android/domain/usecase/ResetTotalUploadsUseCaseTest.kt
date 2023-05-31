package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.transfer.ResetTotalUploadsUseCase
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ResetTotalUploadsUseCaseTest {
    private lateinit var underTest: ResetTotalUploadsUseCase

    private val hasPendingUploads = mock<HasPendingUploads>()
    private val transferRepository = mock<TransferRepository>()

    @Before
    fun setUp() {
        underTest = ResetTotalUploadsUseCase(
            hasPendingUploads = hasPendingUploads,
            transferRepository = transferRepository,
        )
    }

    @Test
    fun `test that resetTotalUploads is invoked when there are no pending uploads`() = runTest {
        whenever(hasPendingUploads()).thenReturn(false)

        underTest()

        verify(transferRepository).resetTotalUploads()
    }

    @Test
    fun `test that resetTotalUploads is not invoked when there are pending uploads`() = runTest {
        whenever(hasPendingUploads()).thenReturn(true)

        underTest()

        verifyNoInteractions(transferRepository)
    }
}