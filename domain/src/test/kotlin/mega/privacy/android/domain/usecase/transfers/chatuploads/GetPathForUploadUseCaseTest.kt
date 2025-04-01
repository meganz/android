package mega.privacy.android.domain.usecase.transfers.chatuploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.domain.repository.PermissionRepository
import mega.privacy.android.domain.usecase.file.DoesUriPathHaveSufficientSpaceUseCase
import mega.privacy.android.domain.usecase.transfers.GetCacheFileForChatUploadUseCase
import mega.privacy.android.domain.usecase.transfers.GetPathForUploadUseCase
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

    private val fileSystemRepository = mock<FileSystemRepository>()

    @BeforeAll
    fun setup() {
        underTest = GetPathForUploadUseCase(
            fileSystemRepository = fileSystemRepository,
        )
    }

    @BeforeEach
    fun resetMocks() = runTest {
        reset(
            fileSystemRepository,
        )
        whenever(fileSystemRepository.isFileUri(any())).thenReturn(false)
        whenever(fileSystemRepository.isContentUri(any())).thenReturn(false)
        whenever(fileSystemRepository.isFilePath(any())).thenReturn(false)
    }

    @ParameterizedTest(name = " and isChatUpload is {0}")
    @ValueSource(booleans = [true, false])
    fun `test that original uri path is returned if uri path represents an existing file`(
        isChatUpload: Boolean,
    ) = runTest {
        val path = "/file.txt"
        val uriPath = UriPath(path)
        whenever(fileSystemRepository.isFilePath(path)).thenReturn(true)
        assertThat(underTest.invoke(uriPath)).isEqualTo(path)
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
        whenever(fileSystemRepository.isFileUri(uri)).thenReturn(true)
        whenever(fileSystemRepository.getFileFromFileUri(uri)).thenReturn(file)
        assertThat(underTest.invoke(uriPath)).isEqualTo(path)
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
        whenever(fileSystemRepository.isFileUri(uri)).thenReturn(true)
        whenever(fileSystemRepository.getFileFromFileUri(uri)).thenReturn(file)
        assertThat(underTest.invoke(uriPath)).isEqualTo(path)
    }
}