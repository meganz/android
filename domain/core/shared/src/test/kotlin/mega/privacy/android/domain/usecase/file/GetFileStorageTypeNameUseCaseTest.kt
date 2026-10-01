package mega.privacy.android.domain.usecase.file

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.file.FileStorageType
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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetFileStorageTypeNameUseCaseTest {
    lateinit var underTest: GetFileStorageTypeNameUseCase

    private val fileSystemRepository = mock<FileSystemRepository>()

    @BeforeAll
    fun setUp() {
        underTest = GetFileStorageTypeNameUseCase(
            fileSystemRepository
        )
    }

    @BeforeEach
    fun cleanUp() {
        reset(fileSystemRepository)
    }

    @Test
    fun `test that uri path value is used to get the storage type when it is a path`() = runTest {
        val path = "/path/file.txt"
        val expected: FileStorageType = FileStorageType.Internal("Pixel")
        whenever(fileSystemRepository.getFileStorageTypeName(path)) doReturn expected

        val actual = underTest(UriPath(path))

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that path from getAbsolutePathByContentUri is used to get the storage type when it is not a path`() =
        runTest {
            val uriPath = UriPath("content://path/file.txt")
            val expected: FileStorageType = FileStorageType.Internal("Pixel")
            val path = "/path"
            whenever(fileSystemRepository.getAbsolutePathByContentUri(uriPath.value)) doReturn path
            whenever(fileSystemRepository.getFileStorageTypeName(path)) doReturn expected

            val actual = underTest(uriPath)

            assertThat(actual).isEqualTo(expected)
        }
}