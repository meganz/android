package mega.privacy.android.domain.usecase.transfer.uploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Test class for [IsThereAnyPendingUploadsUseCase]
 */
@ExperimentalCoroutinesApi
class IsThereAnyPendingUploadsUseCaseTest {

    private lateinit var underTest: IsThereAnyPendingUploadsUseCase

    private val getNumPendingUploadsUseCase = mock<GetNumPendingUploadsUseCase>()

    @Before
    fun setUp() {
        underTest = IsThereAnyPendingUploadsUseCase(getNumPendingUploadsUseCase)
    }

    @Test
    fun `test that has pending uploads returns true if there are pending uploads`() = runTest {
        whenever(getNumPendingUploadsUseCase()).thenReturn(1)
        assertThat(underTest()).isTrue()
    }

    @Test
    fun `test that has pending uploads returns false if there are no pending uploads`() = runTest {
        whenever(getNumPendingUploadsUseCase()).thenReturn(0)
        assertThat(underTest()).isFalse()
    }

    @Test
    fun `test that has pending uploads returns false if the pending uploads count is negative`() =
        runTest {
            whenever(getNumPendingUploadsUseCase()).thenReturn(-1)
            assertThat(underTest()).isFalse()
        }
}