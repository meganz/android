package mega.privacy.android.domain.usecase.node

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class SetNodeDescriptionUseCaseTest {

    private val nodeRepository = mock<NodeRepository>()
    private val useCase = SetNodeDescriptionUseCase(nodeRepository)

    @Test
    fun `invoke should call setNodeDescription from repository`() = runTest {
        val nodeHandle = NodeId(1)
        val description = "description"
        useCase(nodeHandle, description)
        verify(nodeRepository).setNodeDescription(nodeHandle, description)
    }
}