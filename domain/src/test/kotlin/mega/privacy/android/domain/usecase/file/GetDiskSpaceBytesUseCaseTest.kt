package mega.privacy.android.domain.usecase.file

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetDiskSpaceBytesUseCaseTest {
    private lateinit var underTest: GetDiskSpaceBytesUseCase

    private val fileSystemRepository = mock<FileSystemRepository>()

    @BeforeAll
    fun setUp() {
        underTest = GetDiskSpaceBytesUseCase(
            fileSystemRepository = fileSystemRepository
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(fileSystemRepository)
    }


    @Test
    fun `test that path is used to get disk space from file gateway when UriPath is a path`() =
        runTest {
            val path = "/path"
            val expected = 1346L

            whenever(fileSystemRepository.getDiskSpaceBytes(path)) doReturn expected

            val result = underTest(UriPath(path))
            assertThat(result).isEqualTo(expected)
        }

    @Test
    fun `test that uri path is used to get disk space from file gateway when UriPath is a file uri`() =
        runTest {
            val path = "/path"
            val fileUri = "file://$path"
            val expected = 1346L
            val file = mock<File> {
                on { this.absolutePath } doReturn path
            }

            whenever(fileSystemRepository.getDiskSpaceBytes(path)) doReturn expected
            whenever(fileSystemRepository.isFileUri(fileUri)) doReturn true
            whenever(fileSystemRepository.getFileFromFileUri(fileUri)) doReturn file


            val result = underTest(UriPath(fileUri))
            assertThat(result).isEqualTo(expected)
        }

    @Test
    fun `test that path from document file wrapper is used to get disk space from file gateway when UriPath is a content uri`() =
        runTest {
            val path = "/path"
            val contentUri = "content://whatever"
            val expected = 1346L

            whenever(fileSystemRepository.getDiskSpaceBytes(path)) doReturn expected
            whenever(fileSystemRepository.isFileUri(contentUri)) doReturn false
            whenever(fileSystemRepository.getAbsolutePathByContentUri(contentUri)) doReturn path

            val result = underTest(UriPath(contentUri))
            assertThat(result).isEqualTo(expected)
        }

    @Test
    fun `test that path from file gateway is used to get disk space from file gateway when UriPath is a content uri and path from document file wrapper is null`() =
        runTest {
            val path = "/path"
            val contentUri = "content://whatever"
            val expected = 1369876L

            whenever(fileSystemRepository.getDiskSpaceBytes(path)) doReturn expected
            whenever(fileSystemRepository.isFileUri(contentUri)) doReturn false
            whenever(fileSystemRepository.getAbsolutePathByContentUri(contentUri)) doReturn null
            whenever(fileSystemRepository.getExternalPathByUri(contentUri)) doReturn path

            val result = underTest(UriPath(contentUri))
            assertThat(result).isEqualTo(expected)
        }
}