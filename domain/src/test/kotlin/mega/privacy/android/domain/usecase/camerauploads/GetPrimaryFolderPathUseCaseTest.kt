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

/**
 * Test class for [GetPrimaryFolderPathUseCase]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetPrimaryFolderPathUseCaseTest {

    private lateinit var underTest: GetPrimaryFolderPathUseCase

    private val cameraUploadRepository = mock<CameraUploadRepository>()
    private val fileSystemRepository = mock<FileSystemRepository>()
    private val setPrimaryFolderLocalPathUseCase = mock<SetPrimaryFolderLocalPathUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = GetPrimaryFolderPathUseCase(
            cameraUploadRepository = cameraUploadRepository,
            fileSystemRepository = fileSystemRepository,
            setPrimaryFolderLocalPathUseCase = setPrimaryFolderLocalPathUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            cameraUploadRepository,
            fileSystemRepository,
            setPrimaryFolderLocalPathUseCase,
        )
    }

    @ParameterizedTest(name = "is in SD card: {0}")
    @ValueSource(booleans = [true, false])
    fun `test that the primary folder path could be located in the SD card`(isInSDCard: Boolean) =
        runTest {
            val testSDCardPath = "test/sd/card/path"

            cameraUploadRepository.stub {
                onBlocking { isPrimaryFolderInSDCard() }.thenReturn(isInSDCard)
                onBlocking { getPrimaryFolderSDCardUriPath() }.thenReturn(testSDCardPath)
                onBlocking { getPrimaryFolderLocalPath() }.thenReturn("test/local/path")
            }
            fileSystemRepository.stub {
                onBlocking { doesFolderExists(any()) }.thenReturn(true)
                onBlocking { localDCIMFolderPath }.thenReturn("local/DCIM/folder/path")
            }

            underTest()

            if (isInSDCard) {
                assertThat(cameraUploadRepository.getPrimaryFolderSDCardUriPath()).isEqualTo(
                    testSDCardPath
                )
            } else {
                verify(cameraUploadRepository, times(0)).getPrimaryFolderSDCardUriPath()
            }
        }

    @Test
    fun `test that the local path is returned`() = runTest {
        val testLocalPath = "test/local/path"

        cameraUploadRepository.stub {
            onBlocking { isPrimaryFolderInSDCard() }.thenReturn(false)
            onBlocking { getPrimaryFolderLocalPath() }.thenReturn(testLocalPath)
        }
        fileSystemRepository.stub {
            onBlocking { doesFolderExists(any()) }.thenReturn(true)
        }
    }

    @Test
    fun `test that the local path is returned if it is empty and the local and external storage directories do not exist`() =
        runTest {
            val testLocalPath = ""

            cameraUploadRepository.stub {
                onBlocking { isPrimaryFolderInSDCard() }.thenReturn(false)
                onBlocking { getPrimaryFolderLocalPath() }.thenReturn(testLocalPath)
            }
            fileSystemRepository.stub {
                onBlocking { doesFolderExists(any()) }.thenReturn(false)
                onBlocking { doesExternalStorageDirectoryExists() }.thenReturn(false)
            }

            assertThat(underTest()).isEmpty()

        }

    @Test
    fun `test that the local DCIM folder path is returned if the local path is empty`() = runTest {
        val testLocalDCIMPath = "test/local/DCIM/path"

        cameraUploadRepository.stub {
            onBlocking { isPrimaryFolderInSDCard() }.thenReturn(false)
            onBlocking { getPrimaryFolderLocalPath() }.thenReturn("")
        }
        fileSystemRepository.stub {
            onBlocking { doesFolderExists(any()) }.thenReturn(false)
            onBlocking { localDCIMFolderPath }.thenReturn(testLocalDCIMPath)
            onBlocking { doesExternalStorageDirectoryExists() }.thenReturn(true)
        }

        assertThat(underTest()).isEqualTo(testLocalDCIMPath)
        verify(setPrimaryFolderLocalPathUseCase, times(1)).invoke(testLocalDCIMPath)
    }

    @Test
    fun `test that the local DCIM folder path is returned if the folder retrieved from the local path does not exit`() =
        runTest {
            val testLocalDCIMPath = "test/local/DCIM/path"

            cameraUploadRepository.stub {
                onBlocking { isPrimaryFolderInSDCard() }.thenReturn(false)
                onBlocking { getPrimaryFolderLocalPath() }.thenReturn("test/local/path")
            }
            fileSystemRepository.stub {
                onBlocking { doesFolderExists(any()) }.thenReturn(false)
                onBlocking { localDCIMFolderPath }.thenReturn(testLocalDCIMPath)
                onBlocking { doesExternalStorageDirectoryExists() }.thenReturn(true)
            }

            assertThat(underTest()).isEqualTo(testLocalDCIMPath)
            verify(setPrimaryFolderLocalPathUseCase, times(1)).invoke(testLocalDCIMPath)
        }
}