package mega.privacy.android.domain.usecase.foldernode

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IsFolderEmptyUseCaseTest {
    private val nodeRepository: NodeRepository = mock()
    private val underTest = IsFolderEmptyUseCase(nodeRepository)


    @Test
    fun `test that is folder empty use case invoke isFolderEmpty method`() = runTest {
        val node = mock<TypedNode>()
        underTest.invoke(node)
        verify(nodeRepository).isEmptyFolder(node)
    }

}