package mega.privacy.android.domain.usecase.file

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.uri.UriPath
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
 * Test class for [RenameFileAndDeleteOriginalUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RenameFileAndDeleteOriginalUseCaseTest {

    private lateinit var underTest: RenameFileAndDeleteOriginalUseCase

    private val fileSystemRepository = mock<FileSystemRepository>()

    @BeforeAll
    fun setup() {
        underTest = RenameFileAndDeleteOriginalUseCase(fileSystemRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(fileSystemRepository)
    }

    @Test
    fun `test that the use case invokes the expected functionality and returns the renamed file`() =
        runTest {
            val originalUriPath = UriPath("originalUriPath")
            val newFilename = "newFilename"
            val renamedFile = mock<File>()
            whenever(
                fileSystemRepository.renameFileAndDeleteOriginal(
                    originalUriPath = originalUriPath,
                    newFilename = newFilename,
                )
            ).thenReturn(renamedFile)

            assertThat(
                underTest(
                    originalUriPath = originalUriPath,
                    newFilename = newFilename,
                )
            ).isEqualTo(renamedFile)
        }
}