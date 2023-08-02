package mega.privacy.android.domain.usecase.transfer.uploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.TransferRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ResetTotalUploadsUseCaseTest {
    private lateinit var underTest: ResetTotalUploadsUseCase

    private val isThereAnyPendingUploadsUseCase = mock<IsThereAnyPendingUploadsUseCase>()
    private val transferRepository = mock<TransferRepository>()

    @Before
    fun setUp() {
        underTest = ResetTotalUploadsUseCase(
            isThereAnyPendingUploadsUseCase = isThereAnyPendingUploadsUseCase,
            transferRepository = transferRepository,
        )
    }

    @Test
    fun `test that resetTotalUploads is invoked when there are no pending uploads`() = runTest {
        whenever(isThereAnyPendingUploadsUseCase()).thenReturn(false)

        underTest()

        verify(transferRepository).resetTotalUploads()
    }

    @Test
    fun `test that resetTotalUploads is not invoked when there are pending uploads`() = runTest {
        whenever(isThereAnyPendingUploadsUseCase()).thenReturn(true)

        underTest()

        verifyNoInteractions(transferRepository)
    }
}