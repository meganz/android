package mega.privacy.android.domain.usecase

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.repository.FileSystemRepository
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class GetLocalFileForNodeUseCaseTest {
    private val fileSystemRepository: FileSystemRepository = mock()
    private val getLocalFileForNodeUseCase = GetLocalFileForNodeUseCase(fileSystemRepository)

    @Test
    fun `test that getLocalFileForNodeUseCase calls getLocalFile from fileSystemRepository`() =
        runTest {
            val fileNode = mock<TypedFileNode>()
            getLocalFileForNodeUseCase(fileNode)
            verify(fileSystemRepository).getLocalFile(fileNode)
        }

}