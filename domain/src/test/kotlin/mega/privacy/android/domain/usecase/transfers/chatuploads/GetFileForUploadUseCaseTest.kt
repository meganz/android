package mega.privacy.android.domain.usecase.transfers.chatuploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.usecase.file.DoesPathHaveSufficientSpaceUseCase
import mega.privacy.android.domain.usecase.transfers.GetCacheFileForUploadUseCase
import mega.privacy.android.domain.usecase.transfers.GetFileForUploadUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File
import java.io.IOException

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetFileForUploadUseCaseTest {
    private lateinit var underTest: GetFileForUploadUseCase

    private val getCacheFileForUploadUseCase = mock<GetCacheFileForUploadUseCase>()
    private val doesPathHaveSufficientSpaceUseCase = mock<DoesPathHaveSufficientSpaceUseCase>()
    private val fileSystemRepository = mock<FileSystemRepository>()

    @BeforeAll
    fun setup() {
        underTest = GetFileForUploadUseCase(
            getCacheFileForUploadUseCase,
            doesPathHaveSufficientSpaceUseCase,
            fileSystemRepository,
        )
    }

    @BeforeEach
    fun resetMocks() = runTest {
        reset(
            getCacheFileForUploadUseCase,
            doesPathHaveSufficientSpaceUseCase,
            fileSystemRepository,
        )
        whenever(fileSystemRepository.isFileUri(any())).thenReturn(false)
        whenever(fileSystemRepository.isContentUri(any())).thenReturn(false)
        whenever(fileSystemRepository.isFilePath(any())).thenReturn(false)
        whenever(doesPathHaveSufficientSpaceUseCase(any(), any())).thenReturn(true)
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
        val filePath = "/folder/example.txt"
        val file = File(filePath)
        whenever(fileSystemRepository.isContentUri(any())).thenReturn(true)
        whenever(fileSystemRepository.getFileNameFromUri(uriString)).thenReturn(filePath)
        whenever(getCacheFileForUploadUseCase(any(), any())).thenReturn(file)
        assertThat(underTest.invoke(uriString, isChatUpload)).isEqualTo(file)
        verify(fileSystemRepository).copyContentUriToFile(UriPath(uriString), file)
    }

    @ParameterizedTest(name = " and isChatUpload is {0}")
    @ValueSource(booleans = [true, false])
    fun `test that an exception is thrown when uri is a content uri and there's not enough space`(
        isChatUpload: Boolean,
    ) = runTest {
        val uriString = "content://example.txt"
        val filePath = "/folder/example.txt"
        val file = File(filePath)
        whenever(doesPathHaveSufficientSpaceUseCase(any(), any())) doReturn false
        whenever(fileSystemRepository.isContentUri(any())).thenReturn(true)
        whenever(fileSystemRepository.getFileNameFromUri(uriString)).thenReturn(filePath)
        whenever(getCacheFileForUploadUseCase(any(), any())).thenReturn(file)
        assertThrows<IOException> {
            underTest.invoke(uriString, isChatUpload)
        }
        verify(fileSystemRepository, never()).copyContentUriToFile(UriPath(uriString), file)
    }
}