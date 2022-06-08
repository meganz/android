package test.mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.repository.FilesRepository
import mega.privacy.android.app.domain.usecase.DefaultGetRootFolder
import mega.privacy.android.app.domain.usecase.GetRootFolder
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultGetRootFolderTest {

    private lateinit var underTest: GetRootFolder
    private val filesRepository = mock<FilesRepository>()

    @Before
    fun setUp() {
        underTest = DefaultGetRootFolder(filesRepository)
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