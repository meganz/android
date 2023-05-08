package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultGetNodeByIdTest {
    lateinit var underTest: GetNodeById
    private val nodeRepository = mock<NodeRepository>()
    private val addNodeType = mock<AddNodeType>()

    private val unTypeNode = mock<FolderNode>()

    @Before
    fun setUp() {
        underTest = DefaultGetNodeById(nodeRepository, addNodeType)
    }

    @Test
    fun `test that the AddNodeType has been invoked`() =
        runTest {
            val id = NodeId(1L)
            whenever(nodeRepository.getNodeById(id)).thenReturn(unTypeNode)

            underTest(id)

            verify(addNodeType, times(1)).invoke(unTypeNode)
        }
}