package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.FileRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultCheckNodeInRubbishTest {
    private lateinit var underTest: CheckNodeInRubbish
    private val fileRepository = mock<FileRepository>()

    @Before
    fun setUp() {
        underTest = DefaultCheckNodeInRubbish(
            fileRepository = fileRepository,
            ioDispatcher = UnconfinedTestDispatcher()
        )
    }

    @Test
    fun `test that invoke with valid node handle returns true`() = runTest {
        val validHandle = 123456789L
        whenever(fileRepository.checkNodeInRubbish(validHandle)).thenReturn(true)
        assertThat(underTest(validHandle)).isEqualTo(true)
    }

    @Test
    fun `test that invoke with invalid node handle returns false`() = runTest {
        val invalidHandle = 123456789L
        whenever(fileRepository.checkNodeInRubbish(invalidHandle)).thenReturn(false)
        assertThat(underTest(invalidHandle)).isEqualTo(false)
    }
}