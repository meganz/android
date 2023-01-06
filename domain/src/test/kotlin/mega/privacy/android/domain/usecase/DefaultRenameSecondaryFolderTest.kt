package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultRenameSecondaryFolderTest {
    private lateinit var underTest: RenameSecondaryFolder
    private val cameraUploadRepository = mock<CameraUploadRepository>()
    private val getCameraUploadFolderName = mock<GetCameraUploadFolderName>()
    private val englishName = "Media Uploads"
    private val notEnglishName = "Telechargements"
    private val handle = 9L

    @Before
    fun setUp() {
        underTest = DefaultRenameSecondaryFolder(
            cameraUploadRepository = cameraUploadRepository,
            getCameraUploadFolderName = getCameraUploadFolderName
        )
    }

    @Test
    fun `test that secondary camera upload folder does not get renamed if english already`() =
        runTest {
            whenever(getCameraUploadFolderName(any())).thenReturn(englishName)
            underTest(handle)
            verifyNoInteractions(cameraUploadRepository)
        }

    @Test
    fun `test that secondary camera upload folder gets renamed if not english`() =
        runTest {
            whenever(getCameraUploadFolderName(any())).thenReturn(notEnglishName)
            underTest(handle)
            verify(cameraUploadRepository, times(1)).renameNode(handle, notEnglishName)
        }
}
