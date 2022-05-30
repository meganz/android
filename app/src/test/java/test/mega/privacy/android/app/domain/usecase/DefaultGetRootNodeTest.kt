package test.mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.repository.FilesRepository
import mega.privacy.android.app.domain.usecase.DefaultGetRootNode
import mega.privacy.android.app.domain.usecase.GetRootNode
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultGetRootNodeTest {

    private lateinit var underTest: GetRootNode
    private val filesRepository = mock<FilesRepository>()

    @Before
    fun setUp() {
        underTest = DefaultGetRootNode(filesRepository)
    }

    @Test
    fun `test that get root node repository is invoked when underTest is invoked`() = runTest {
        underTest()

        verify(filesRepository).getRootNode()
    }

    @Test
    fun `test that underTest returns the value of get root node repository if not null`() = runTest {
        whenever(filesRepository.getRootNode()).thenReturn(mock())

        assertTrue { underTest() != null }
    }

    @Test
    fun `test that underTest returns null if the value of get root node repository is null`() = runTest {
        whenever(filesRepository.getRootNode()).thenReturn(null)

        assertTrue { underTest() == null }
    }
}