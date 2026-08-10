package mega.privacy.android.domain.usecase.file

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import java.io.File

/**
 * Test class for [SaveFileToDestinationUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SaveFileToDestinationUseCaseTest {
    private lateinit var underTest: SaveFileToDestinationUseCase
    private val fileSystemRepository = mock<FileSystemRepository>()

    @BeforeAll
    fun setUp() {
        underTest = SaveFileToDestinationUseCase(
            fileSystemRepository = fileSystemRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(fileSystemRepository)
    }

    @Test
    fun `test that use case calls repository with correct parameters`() = runTest {
        val sourceFile = File("/test/source/file.txt")
        val destinationUri =
            UriPath("content://com.android.externalstorage.documents/tree/primary%3ADownload")

        //whenever(fileSystemRepository.copyFilesToDocumentUri(sourceFile, destinationUri)).thenReturn(1)

        underTest(sourceFile, destinationUri)

        verify(fileSystemRepository).copyFilesToDocumentUri(sourceFile, destinationUri)
    }
}

