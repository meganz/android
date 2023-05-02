package mega.privacy.android.domain.usecase.camerauploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CameraUploadRepository
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

/**
 * Test class for [GetSecondaryFolderPathUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetSecondaryFolderPathUseCaseTest {

    private lateinit var underTest: GetSecondaryFolderPathUseCase

    private val cameraUploadRepository = mock<CameraUploadRepository>()
    private val fileSystemRepository = mock<FileSystemRepository>()

    @BeforeAll
    fun setUp() {
        underTest = GetSecondaryFolderPathUseCase(
            cameraUploadRepository = cameraUploadRepository,
            fileSystemRepository = fileSystemRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            cameraUploadRepository,
            fileSystemRepository,
        )
    }

    @ParameterizedTest(name = "is in SD card: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that the secondary folder path could be located in the SD card`(isInSDCard: Boolean) =
        runTest {
            val testSDCardPath = "test/sd/card/path"

            cameraUploadRepository.stub {
                onBlocking { isSecondaryFolderInSDCard() }.thenReturn(isInSDCard)
                onBlocking { getSecondaryFolderSDCardUriPath() }.thenReturn(testSDCardPath)
            }

            if (isInSDCard) {
                assertThat(underTest()).isEqualTo(testSDCardPath)
                verifyNoInteractions(fileSystemRepository)
            } else {
                verify(cameraUploadRepository, times(0)).getPrimaryFolderSDCardUriPath()
            }
        }

    @ParameterizedTest(name = "path: {0}")
    @ValueSource(strings = ["", " ", "test/path"])
    fun `test that the local secondary folder path is returned`(path: String) = runTest {
        cameraUploadRepository.stub {
            onBlocking { isSecondaryFolderInSDCard() }.thenReturn(false)
            onBlocking { getSecondaryFolderLocalPath() }.thenReturn(path)
        }
        fileSystemRepository.stub {
            onBlocking { doesFolderExists(any()) }.thenReturn(true)
        }

        assertThat(underTest()).isEqualTo(path)
    }

    @Test
    fun `test that an empty secondary folder path is set and returned if the previously set path does not exist`() =
        runTest {
            val testPath = "test/path"

            cameraUploadRepository.stub {
                onBlocking { isSecondaryFolderInSDCard() }.thenReturn(false)
                onBlocking { getSecondaryFolderLocalPath() }.thenReturn(testPath)
            }
            fileSystemRepository.stub {
                onBlocking { doesFolderExists(any()) }.thenReturn(false)
            }

            val expected = underTest()
            assertThat(expected).isNotEqualTo(testPath)
            assertThat(expected).isEmpty()
            verify(cameraUploadRepository, times(1)).setSecondaryFolderLocalPath("")
        }
}