package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultHasAncestorTest {
    private lateinit var underTest: HasAncestor

    private val nodeRepository = mock<NodeRepository>()
    private val targetNode = NodeId(12L)

    @Before
    fun setUp() {
        underTest = DefaultHasAncestor(nodeRepository = nodeRepository)
    }

    @Test
    fun `test that true is returned if the ids are the same`() = runTest {
        val actual = underTest(targetNode, targetNode)
        assertThat(actual).isTrue()
    }

    @Test
    fun `test that true is returned if direct parent is the ancestor`() = runTest {
        val ancestor = NodeId(targetNode.longValue + 1)

        nodeRepository.stub {
            onBlocking { getParentNodeId(targetNode) }.thenReturn(ancestor)
        }
        val actual = underTest(targetNode, ancestor)
        assertThat(actual).isTrue()
    }

    @Test
    fun `test that false is returned if the target node is null`() = runTest {
        nodeRepository.stub {
            onBlocking { getParentNodeId(targetNode) }.thenReturn(null)
        }
        val actual = underTest(targetNode, NodeId(13L))
        assertThat(actual).isFalse()
    }

    @Test
    fun `test that true is returned if the parent of the direct parent is the ancestor`() =
        runTest {
            val ancestor = NodeId(targetNode.longValue + 1)
            val directParentId = NodeId(ancestor.longValue + 1)

            nodeRepository.stub {
                onBlocking { getParentNodeId(targetNode) }.thenReturn(directParentId)
                onBlocking { getParentNodeId(directParentId) }.thenReturn(ancestor)
            }
            val actual = underTest(targetNode, ancestor)
            assertThat(actual).isTrue()
        }

    @Test
    fun `test that true is returned if distant ancestor is the ancestor`() = runTest {
        val ancestor = NodeId(targetNode.longValue + 1)
        val directParentId = NodeId(ancestor.longValue + 1)

        nodeRepository.stub {
            onBlocking { getParentNodeId(targetNode) }.thenReturn(directParentId)
            onBlocking { getParentNodeId(directParentId) }
                .thenReturn(directParentId)
                .thenReturn(directParentId)
                .thenReturn(directParentId)
                .thenReturn(directParentId)
                .thenReturn(ancestor)
        }
        val actual = underTest(targetNode, ancestor)
        assertThat(actual).isTrue()
    }
}