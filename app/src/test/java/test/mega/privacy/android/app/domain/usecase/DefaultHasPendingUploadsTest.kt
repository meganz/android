package test.mega.privacy.android.app.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.usecase.DefaultHasPendingUploads
import mega.privacy.android.domain.usecase.GetNumPendingUploads
import mega.privacy.android.domain.usecase.HasPendingUploads
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Test class for [HasPendingUploads]
 */
@ExperimentalCoroutinesApi
class DefaultHasPendingUploadsTest {

    private lateinit var underTest: HasPendingUploads

    private val getNumPendingUploads = mock<GetNumPendingUploads>()

    @Before
    fun setUp() {
        underTest = DefaultHasPendingUploads(getNumPendingUploads)
    }

    @Test
    fun `test that has pending uploads returns true if there are pending uploads`() = runTest {
        whenever(getNumPendingUploads()).thenReturn(1)
        assertThat(underTest()).isTrue()
    }

    @Test
    fun `test that has pending uploads returns false if there are no pending uploads`() = runTest {
        whenever(getNumPendingUploads()).thenReturn(0)
        assertThat(underTest()).isFalse()
    }

    @Test
    fun `test that has pending uploads returns false if the pending uploads count is negative`() =
        runTest {
            whenever(getNumPendingUploads()).thenReturn(-1)
            assertThat(underTest()).isFalse()
        }
}