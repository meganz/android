package mega.privacy.android.domain.usecase.file

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetFileSizeFromUriPathUseCaseTest {
    private lateinit var underTest: GetFileSizeFromUriPathUseCase

    private val fileSystemRepository = mock<FileSystemRepository>()

    @BeforeAll
    fun setup() {
        underTest = GetFileSizeFromUriPathUseCase(fileSystemRepository)
    }

    @BeforeEach
    fun cleanUp() = runTest {
        reset(fileSystemRepository)
        whenever(fileSystemRepository.isFilePath(any())).thenReturn(false)
        whenever(fileSystemRepository.isFolderPath(any())).thenReturn(false)
        whenever(fileSystemRepository.isFileUri(any())).thenReturn(false)
        whenever(fileSystemRepository.isContentUri(any())).thenReturn(false)

    }

    @Test
    fun `test that folder result is returned when uri path is a path to a folder`() = runTest {
        val filePath = "/path/to/folder"
        whenever(fileSystemRepository.isFolderPath(filePath)).thenReturn(true)

        val actual = underTest(UriPath(filePath))

        assertThat(actual).isEqualTo(FolderResult)
    }

    @Test
    fun `test that file size is returned when uri path is a path to a file`() = runTest {
        val filePath = "/path/to/file.txt"
        val size = 1024L
        val file = mock<File> {
            on { length() } doReturn size
        }
        whenever(fileSystemRepository.isFilePath(filePath)).thenReturn(true)
        whenever(fileSystemRepository.getFileByPath(filePath)).thenReturn(file)

        val actual = underTest(UriPath(filePath))

        assertThat(actual).isEqualTo(FileResult(size))
    }

    @Test
    fun `test that unknown is returned when uri path is a path to a no existing file`() = runTest {
        val filePath = "/path/to/nonexistent.txt"
        whenever(fileSystemRepository.isFilePath(filePath)).thenReturn(true)
        whenever(fileSystemRepository.getFileByPath(filePath)).thenReturn(null)

        val actual = underTest(UriPath(filePath))

        assertThat(actual).isEqualTo(UnknownResult)
    }

    @Test
    fun `test that file size is returned when uri path is a file uri`() = runTest {
        val fileUriString = "file:///path/to/file.txt"
        val fileSize = 2024L
        val file = mock<File> {
            on { length() } doReturn fileSize
            on { isFile } doReturn true
        }
        whenever(fileSystemRepository.isFileUri(fileUriString)).thenReturn(true)
        whenever(fileSystemRepository.getFileFromFileUri(fileUriString)).thenReturn(file)

        val actual = underTest(UriPath(fileUriString))

        assertThat(actual).isEqualTo(FileResult(fileSize))
    }

    @Test
    fun `test that Folder is returned when uri path is a folder uri`() = runTest {
        val fileUriString = "file:///path/to/folder"
        val file = mock<File> {
            on { isFile } doReturn false
            on { isDirectory } doReturn true
        }
        whenever(fileSystemRepository.isFileUri(fileUriString)).thenReturn(true)
        whenever(fileSystemRepository.getFileFromFileUri(fileUriString)).thenReturn(file)

        val actual = underTest(UriPath(fileUriString))

        assertThat(actual).isEqualTo(FolderResult)
    }

    @Test
    fun `test that Unknown is returned when uri path is an unknown uri`() = runTest {
        val fileUriString = "file:///path/to/folder"
        val file = mock<File> {
            on { isFile } doReturn false
            on { isDirectory } doReturn false
        }
        whenever(fileSystemRepository.isFileUri(fileUriString)).thenReturn(true)
        whenever(fileSystemRepository.getFileFromFileUri(fileUriString)).thenReturn(file)

        val actual = underTest(UriPath(fileUriString))

        assertThat(actual).isEqualTo(UnknownResult)
    }

    @Test
    fun `test that file size from uri is returned when uri path is a content uri`() = runTest {
        val contentUriString = "content://some_provider/file"
        val fileSize = 4096L
        whenever(fileSystemRepository.isContentUri(contentUriString)).thenReturn(true)
        whenever(fileSystemRepository.getFileSizeFromUri(contentUriString)).thenReturn(fileSize)

        val actual = underTest(UriPath(contentUriString))

        assertThat(actual).isEqualTo(FileResult(fileSize))
    }

    @Test
    fun `test that 0 is returned when uri path is no compatible`() = runTest {
        val actual = underTest(UriPath("unknown://file.txt"))

        assertThat(actual).isEqualTo(UnknownResult)
    }
}