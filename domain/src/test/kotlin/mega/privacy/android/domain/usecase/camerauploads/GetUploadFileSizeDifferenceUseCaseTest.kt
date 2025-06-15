package mega.privacy.android.domain.usecase.camerauploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.camerauploads.CameraUploadsRecord
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.file.GetFileByPathUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.io.File

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetUploadFileSizeDifferenceUseCaseTest {

    private lateinit var underTest: GetUploadFileSizeDifferenceUseCase

    private val fileSystemRepository = mock<FileSystemRepository>()
    private val getFileByPathUseCase = mock<GetFileByPathUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = GetUploadFileSizeDifferenceUseCase(
            fileSystemRepository = fileSystemRepository,
            getFileByPathUseCase = getFileByPathUseCase
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            fileSystemRepository,
            getFileByPathUseCase
        )
    }

    @Test
    fun `test that difference is null when temp file is not exist`() = runTest {
        val mockFilePath = "temp/file/path"
        val record = mock<CameraUploadsRecord> {
            on { tempFilePath }.thenReturn(mockFilePath)
        }
        whenever(fileSystemRepository.doesFileExist(mockFilePath)).thenReturn(false)
        val actual = underTest(record)
        assertThat(actual).isNull()
    }

    @Test
    fun `test that difference is null when getFileByPathUseCase returns null`() = runTest {
        val mockFilePath = "temp/file/path"
        val record = mock<CameraUploadsRecord> {
            on { tempFilePath }.thenReturn(mockFilePath)
        }
        whenever(fileSystemRepository.doesFileExist(mockFilePath)).thenReturn(true)
        whenever(getFileByPathUseCase(mockFilePath)).thenReturn(null)
        val actual = underTest(record)
        assertThat(actual).isNull()
    }

    @Test
    fun `test that difference is null when original file size is equal temp file size`() = runTest {
        val mockFilePath = "temp/file/path"
        val originalFileSize = 100L
        val record = mock<CameraUploadsRecord> {
            on { tempFilePath }.thenReturn(mockFilePath)
            on { fileSize }.thenReturn(originalFileSize)
        }
        val file = mock<File> {
            on { length() }.thenReturn(originalFileSize)
        }
        whenever(fileSystemRepository.doesFileExist(mockFilePath)).thenReturn(true)
        whenever(getFileByPathUseCase(mockFilePath)).thenReturn(file)
        val actual = underTest(record)
        assertThat(actual).isNull()
    }

    @Test
    fun `test that difference is as expected`() = runTest {
        val mockFilePath = "temp/file/path"
        val originalFileSize = 100L
        val tempFileSize = 80L
        val record = mock<CameraUploadsRecord> {
            on { tempFilePath }.thenReturn(mockFilePath)
            on { fileSize }.thenReturn(originalFileSize)
        }
        val file = mock<File> {
            on { length() }.thenReturn(tempFileSize)
        }
        whenever(fileSystemRepository.doesFileExist(mockFilePath)).thenReturn(true)
        whenever(getFileByPathUseCase(mockFilePath)).thenReturn(file)
        val actual = underTest(record)
        assertThat(actual).isEqualTo(originalFileSize - tempFileSize)
    }
}