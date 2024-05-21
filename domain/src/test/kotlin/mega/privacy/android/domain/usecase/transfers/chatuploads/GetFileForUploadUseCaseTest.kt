package mega.privacy.android.domain.usecase.transfers.chatuploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.transfers.GetCacheFileForUploadUseCase
import mega.privacy.android.domain.usecase.transfers.GetFileForUploadUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetFileForUploadUseCaseTest {
    private lateinit var underTest: GetFileForUploadUseCase

    private val getCacheFileForUploadUseCase = mock<GetCacheFileForUploadUseCase>()
    private val fileSystemRepository = mock<FileSystemRepository>()

    @BeforeAll
    fun setup() {
        underTest = GetFileForUploadUseCase(
            getCacheFileForUploadUseCase,
            fileSystemRepository,
        )
    }

    @BeforeEach
    fun resetMocks() = runTest {
        reset(
            getCacheFileForUploadUseCase,
            fileSystemRepository,
        )
        whenever(fileSystemRepository.isFileUri(any())).thenReturn(false)
        whenever(fileSystemRepository.isContentUri(any())).thenReturn(false)
        whenever(fileSystemRepository.isFilePath(any())).thenReturn(false)
    }

    @ParameterizedTest(name = " and isChatUpload is {0}")
    @ValueSource(booleans = [true, false])
    fun `test that file is returned if path represents an existing file`(
        isChatUpload: Boolean,
    ) = runTest {
        val path = "/file.txt"
        val file = mock<File>()
        whenever(fileSystemRepository.isFilePath(path)).thenReturn(true)
        whenever(fileSystemRepository.getFileByPath(path)).thenReturn(file)
        assertThat(underTest.invoke(path, isChatUpload)).isEqualTo(file)
    }

    @ParameterizedTest(name = " and isChatUpload is {0}")
    @ValueSource(booleans = [true, false])
    fun `test that file from uri is returned when uri represents a file`(
        isChatUpload: Boolean,
    ) = runTest {
        val uri = "file://file.txt"
        val file = mock<File>()
        whenever(fileSystemRepository.isFileUri(uri)).thenReturn(true)
        whenever(fileSystemRepository.getFileFromFileUri(uri)).thenReturn(file)
        assertThat(underTest.invoke(uri, isChatUpload)).isEqualTo(file)
    }

    @ParameterizedTest(name = " and isChatUpload is {0}")
    @ValueSource(booleans = [true, false])
    fun `test that a copy of the file is returned when uri is a content uri`(
        isChatUpload: Boolean,
    ) = runTest {
        val uriString = "content://example.txt"
        val fileName = "example.txt"
        val file = mock<File>()
        whenever(fileSystemRepository.isContentUri(any())).thenReturn(true)
        whenever(fileSystemRepository.getFileNameFromUri(uriString)).thenReturn(fileName)
        whenever(getCacheFileForUploadUseCase(any(), any())).thenReturn(file)
        assertThat(underTest.invoke(uriString, isChatUpload)).isEqualTo(file)
        verify(fileSystemRepository).copyContentUriToFile(uriString, file)
    }
}