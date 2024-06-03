package mega.privacy.android.domain.usecase.camerauploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.io.File

/**
 * Test class for [CreateCameraUploadsTemporaryRootDirectoryUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CreateCameraUploadsTemporaryRootDirectoryUseCaseTest {

    private lateinit var underTest: CreateCameraUploadsTemporaryRootDirectoryUseCase

    private val fileSystemRepository = mock<FileSystemRepository>()

    @BeforeAll
    fun setUp() {
        underTest = CreateCameraUploadsTemporaryRootDirectoryUseCase(fileSystemRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(fileSystemRepository)
    }

    @Test
    fun `test that an empty root directory path is returned when creating the root directory fails`() =
        runTest {
            whenever(fileSystemRepository.createCameraUploadsTemporaryRootDirectory()).thenReturn(
                null
            )
            assertThat(underTest()).isEmpty()
        }

    @Test
    fun `test that the root directory path is returned`() = runTest {
        val directoryPath = "directory/path"
        val directoryFile = mock<File> {
            on { absolutePath }.thenReturn(directoryPath)
        }

        whenever(fileSystemRepository.createCameraUploadsTemporaryRootDirectory()).thenReturn(
            directoryFile
        )

        assertThat(underTest()).isEqualTo("$directoryPath/")
    }
}