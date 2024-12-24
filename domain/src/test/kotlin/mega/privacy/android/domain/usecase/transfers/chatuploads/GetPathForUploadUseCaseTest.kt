package mega.privacy.android.domain.usecase.transfers.chatuploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.PermissionRepository
import mega.privacy.android.domain.usecase.file.DoesPathHaveSufficientSpaceUseCase
import mega.privacy.android.domain.usecase.transfers.GetCacheFileForUploadUseCase
import mega.privacy.android.domain.usecase.transfers.GetPathForUploadUseCase
import mega.privacy.android.domain.usecase.transfers.uploads.UseContentUrisForUploadsUseCase
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
import kotlin.test.Test

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetPathForUploadUseCaseTest {
    private lateinit var underTest: GetPathForUploadUseCase

    private val getCacheFileForUploadUseCase = mock<GetCacheFileForUploadUseCase>()
    private val doesPathHaveSufficientSpaceUseCase = mock<DoesPathHaveSufficientSpaceUseCase>()
    private val fileSystemRepository = mock<FileSystemRepository>()
    private val permissionRepository = mock<PermissionRepository>()
    val useContentUrisForUploadsUseCase = mock<UseContentUrisForUploadsUseCase>()

    @BeforeAll
    fun setup() {
        underTest = GetPathForUploadUseCase(
            getCacheFileForUploadUseCase = getCacheFileForUploadUseCase,
            doesPathHaveSufficientSpaceUseCase = doesPathHaveSufficientSpaceUseCase,
            fileSystemRepository = fileSystemRepository,
            permissionRepository = permissionRepository,
            useContentUrisForUploadsUseCase = useContentUrisForUploadsUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() = runTest {
        reset(
            getCacheFileForUploadUseCase,
            doesPathHaveSufficientSpaceUseCase,
            fileSystemRepository,
            permissionRepository,
            useContentUrisForUploadsUseCase,
        )
        whenever(fileSystemRepository.isFileUri(any())).thenReturn(false)
        whenever(fileSystemRepository.isContentUri(any())).thenReturn(false)
        whenever(fileSystemRepository.isFilePath(any())).thenReturn(false)
        whenever(doesPathHaveSufficientSpaceUseCase(any(), any())).thenReturn(true)
        whenever(useContentUrisForUploadsUseCase(any())).thenReturn(false)
    }

    @ParameterizedTest(name = " and isChatUpload is {0}")
    @ValueSource(booleans = [true, false])
    fun `test that original uri path is returned if uri path represents an existing file`(
        isChatUpload: Boolean,
    ) = runTest {
        val path = "/file.txt"
        val uriPath = UriPath(path)
        whenever(permissionRepository.hasManageExternalStoragePermission()).thenReturn(false)
        whenever(fileSystemRepository.isFilePath(path)).thenReturn(true)
        assertThat(underTest.invoke(uriPath, isChatUpload)).isEqualTo(path)
    }

    @ParameterizedTest(name = " and isChatUpload is {0}")
    @ValueSource(booleans = [true, false])
    fun `test that file from uri is returned when uri represents a file`(
        isChatUpload: Boolean,
    ) = runTest {
        val uri = "file://file.txt"
        val path = "/file.txt"
        val uriPath = UriPath(uri)
        val file = mock<File> {
            on { this.absolutePath } doReturn path
        }
        whenever(permissionRepository.hasManageExternalStoragePermission()).thenReturn(false)
        whenever(fileSystemRepository.isFileUri(uri)).thenReturn(true)
        whenever(fileSystemRepository.getFileFromFileUri(uri)).thenReturn(file)
        assertThat(underTest.invoke(uriPath, isChatUpload)).isEqualTo(path)
    }

    @Test
    fun `test that the uri is returned when a a given content uri should be used`(
    ) = runTest {
        val uri = "file://file.txt"
        val path = "/file.txt"
        val uriPath = UriPath(uri)
        val file = mock<File> {
            on { this.absolutePath } doReturn path
        }
        whenever(useContentUrisForUploadsUseCase(false)).thenReturn(true)
        whenever(permissionRepository.hasManageExternalStoragePermission()).thenReturn(false)
        whenever(fileSystemRepository.isFileUri(uri)).thenReturn(true)
        whenever(fileSystemRepository.getFileFromFileUri(uri)).thenReturn(file)
        assertThat(underTest.invoke(uriPath, false)).isEqualTo(path)
    }

    @ParameterizedTest(name = " and isChatUpload is {0}")
    @ValueSource(booleans = [true, false])
    fun `test that a copy of the file is returned when uri is a content uri`(
        isChatUpload: Boolean,
    ) = runTest {
        val uriString = "content://example.txt"
        val filePath = "/folder/example.txt"
        val file = File(filePath)
        whenever(permissionRepository.hasManageExternalStoragePermission()).thenReturn(false)
        whenever(fileSystemRepository.isContentUri(any())).thenReturn(true)
        whenever(fileSystemRepository.getFileNameFromUri(uriString)).thenReturn(filePath)
        whenever(getCacheFileForUploadUseCase(any(), any())).thenReturn(file)
        assertThat(underTest.invoke(UriPath(uriString), isChatUpload)).isEqualTo(filePath)
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
        whenever(permissionRepository.hasManageExternalStoragePermission()).thenReturn(false)
        whenever(doesPathHaveSufficientSpaceUseCase(any(), any())) doReturn false
        whenever(fileSystemRepository.isContentUri(any())).thenReturn(true)
        whenever(fileSystemRepository.getFileNameFromUri(uriString)).thenReturn(filePath)
        whenever(getCacheFileForUploadUseCase(any(), any())).thenReturn(file)
        assertThrows<IOException> {
            underTest.invoke(UriPath(uriString), isChatUpload)
        }
        verify(fileSystemRepository, never()).copyContentUriToFile(UriPath(uriString), file)
    }

    @ParameterizedTest(name = " and isChatUpload is {0}")
    @ValueSource(booleans = [true, false])
    fun `test that the file is returned when uri is a content uri and permission is enabled`(
        isChatUpload: Boolean,
    ) = runTest {
        val uriPath = UriPath("content://example.txt")
        val filePath = "/folder/example.txt"
        val file = File(filePath)
        whenever(permissionRepository.hasManageExternalStoragePermission()).thenReturn(true)
        whenever(fileSystemRepository.isContentUri(any())).thenReturn(true)
        whenever(fileSystemRepository.getFileFromUri(uriPath)).thenReturn(file)
        assertThat(underTest.invoke(uriPath, isChatUpload)).isEqualTo(filePath)
    }

    @ParameterizedTest(name = " and isChatUpload is {0}")
    @ValueSource(booleans = [true, false])
    fun `test that a copy of the file is returned when uri is a content uri, permission is enabled and can't find the file from uri`(
        isChatUpload: Boolean,
    ) = runTest {
        val uriString = "content://example.txt"
        val uriPath = UriPath(uriString)
        val filePath = "/folder/example.txt"
        val file = File(filePath)
        whenever(permissionRepository.hasManageExternalStoragePermission()).thenReturn(true)
        whenever(fileSystemRepository.isContentUri(any())).thenReturn(true)
        whenever(fileSystemRepository.getFileFromUri(uriPath)).thenReturn(null)
        whenever(fileSystemRepository.getFileNameFromUri(uriString)).thenReturn(filePath)
        whenever(getCacheFileForUploadUseCase(any(), any())).thenReturn(file)
        assertThat(underTest.invoke(uriPath, isChatUpload)).isEqualTo(filePath)
        verify(fileSystemRepository).copyContentUriToFile(UriPath(uriString), file)
    }
}