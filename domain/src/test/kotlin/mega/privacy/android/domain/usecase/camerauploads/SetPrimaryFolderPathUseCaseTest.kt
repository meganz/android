package mega.privacy.android.domain.usecase.camerauploads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

/**
 * Test class for [SetPrimaryFolderPathUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetPrimaryFolderPathUseCaseTest {
    private lateinit var underTest: SetPrimaryFolderPathUseCase

    private val cameraUploadRepository = mock<CameraUploadRepository>()
    private val setPrimaryFolderLocalPathUseCase = mock<SetPrimaryFolderLocalPathUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = SetPrimaryFolderPathUseCase(
            cameraUploadRepository = cameraUploadRepository,
            setPrimaryFolderLocalPathUseCase = setPrimaryFolderLocalPathUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            cameraUploadRepository,
            setPrimaryFolderLocalPathUseCase,
        )
    }

    @ParameterizedTest(name = "is in SD Card: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that the new primary folder path is set`(isInSDCard: Boolean) = runTest {
        val testPath = "test/new/primary/folder/path"
        val testSDCardURIPath = "test/sd/card/uri/path"

        cameraUploadRepository.stub {
            onBlocking { isPrimaryFolderInSDCard() }.thenReturn(isInSDCard)
            onBlocking { getPrimaryFolderSDCardUriPath() }.thenReturn(testSDCardURIPath)
        }

        underTest(testPath)

        verify(cameraUploadRepository, times(1)).setPrimaryFolderInSDCard(isInSDCard)
        if (isInSDCard) {
            verify(cameraUploadRepository, times(1)).getPrimaryFolderSDCardUriPath()
            verify(setPrimaryFolderLocalPathUseCase, times(1)).invoke(testSDCardURIPath)
        } else {
            verify(cameraUploadRepository, times(0)).getPrimaryFolderSDCardUriPath()
            verify(setPrimaryFolderLocalPathUseCase, times(1)).invoke(testPath)
        }
    }
}