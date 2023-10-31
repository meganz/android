package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class UpdateNodeLabelUseCaseTest {

    private val nodeRepository: NodeRepository = mock()
    private val underTest = UpdateNodeLabelUseCase(nodeRepository)

    @Test
    fun `test that setNodeLabel will be called if label is not empty`() =
        runTest {
            val nodeId = NodeId(1L)
            underTest(nodeId = nodeId, label = 1)
            verify(nodeRepository).setNodeLabel(nodeId, 1)
            verify(nodeRepository, times(0)).resetNodeLabel(nodeId)
        }

    @Test
    fun `test that reSetNodeLabel will be called if label is null`() =
        runTest {
            val nodeId = NodeId(1L)
            underTest(nodeId = nodeId, label = null)
            verify(nodeRepository).resetNodeLabel(nodeId)
        }
}