package mega.privacy.android.domain.usecase.camerauploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.whenever

/**
 * Test class for [IsSecondaryFolderSetUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IsSecondaryFolderSetUseCaseTest {

    private lateinit var underTest: IsSecondaryFolderSetUseCase

    private val cameraUploadRepository = mock<CameraUploadRepository>()
    private val fileSystemRepository = mock<FileSystemRepository>()
    private val getSecondaryFolderPathUseCase = mock<GetSecondaryFolderPathUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = IsSecondaryFolderSetUseCase(
            cameraUploadRepository = cameraUploadRepository,
            fileSystemRepository = fileSystemRepository,
            getSecondaryFolderPathUseCase = getSecondaryFolderPathUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(cameraUploadRepository, fileSystemRepository, getSecondaryFolderPathUseCase)
    }

    @ParameterizedTest(name = "folder exists: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that the secondary folder in the SD Card exists`(folderExists: Boolean) = runTest {
        val testPath = "test/path"

        cameraUploadRepository.stub {
            onBlocking { getSecondaryFolderSDCardUriPath() }.thenReturn(testPath)
            onBlocking { isSecondaryFolderInSDCard() }.thenReturn(true)
        }
        whenever(fileSystemRepository.isFolderInSDCardAvailable(testPath)).thenReturn(folderExists)

        assertThat(underTest()).isEqualTo(folderExists)
    }

    @ParameterizedTest(name = "folder exists: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that the local secondary folder exists`(folderExists: Boolean) = runTest {
        val testPath = "test/path"

        whenever(cameraUploadRepository.isSecondaryFolderInSDCard()).thenReturn(false)
        whenever(fileSystemRepository.doesFolderExists(testPath)).thenReturn(folderExists)
        whenever(getSecondaryFolderPathUseCase()).thenReturn(testPath)

        assertThat(underTest()).isEqualTo(folderExists)
    }
}