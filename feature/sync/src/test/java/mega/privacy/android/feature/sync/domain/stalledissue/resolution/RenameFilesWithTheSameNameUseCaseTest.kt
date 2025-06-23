package mega.privacy.android.feature.sync.domain.stalledissue.resolution

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.FileSystemRepository
import mega.privacy.android.feature.sync.domain.usecase.stalledIssue.resolution.RenameFilesWithTheSameNameUseCase
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class RenameFilesWithTheSameNameUseCaseTest {

    private val fileSystemRepository = mock<FileSystemRepository>()

    private val underTest = RenameFilesWithTheSameNameUseCase(fileSystemRepository)

    @Test
    fun `test that when invoked fileSystemRepository's renameDocumentWithTheSameName is called`() =
        runTest {
            val uriPaths = listOf(
                "content://path/to/file%301.txt",
                "content://path/to/file01.txt",
            ).map { UriPath(it) }
            underTest(uriPaths)
            verify(fileSystemRepository).renameDocumentWithTheSameName(uriPaths.drop(1))
        }
}
