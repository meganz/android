package test.mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.domain.repository.FilesRepository
import mega.privacy.android.app.domain.usecase.DefaultGetRubbishBinFolder
import mega.privacy.android.app.domain.usecase.GetRubbishBinFolder
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultGetRubbishBinFolderTest {

    private lateinit var underTest: GetRubbishBinFolder
    private val filesRepository = mock<FilesRepository>()

    @Before
    fun setUp() {
        underTest = DefaultGetRubbishBinFolder(filesRepository)
    }

    @Test
    fun `test that get rubbish bin node repository is invoked when underTest is invoked`() = runTest {
        underTest()

        verify(filesRepository).getRubbishBinNode()
    }

    @Test
    fun `test that underTest returns the value of get rubbish bin node repository if not null`() = runTest {
        whenever(filesRepository.getRubbishBinNode()).thenReturn(mock())

        assertTrue { underTest() != null }
    }

    @Test
    fun `test that underTest returns null if the value of get rubbish bin node repository is null`() = runTest {
        whenever(filesRepository.getRubbishBinNode()).thenReturn(null)

        assertTrue { underTest() == null }
    }
}