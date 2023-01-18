package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.FileRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultHasAncestorTest {
    private lateinit var underTest: HasAncestor

    private val fileRepository = mock<FileRepository>()
    private val targetNode = NodeId(12L)

    @Before
    fun setUp() {
        underTest = DefaultHasAncestor(fileRepository = fileRepository)
    }

    @Test
    fun `test that true is returned if the ids are the same`() = runTest {
        val Node = mock<Node> { on { id }.thenReturn(targetNode) }

        fileRepository.stub {
            onBlocking { getNodeById(targetNode) }.thenReturn(Node)
        }

        val actual = underTest(targetNode, targetNode)
        assertThat(actual).isTrue()
    }

    @Test
    fun `test that true is returned if direct parent is the ancestor`() = runTest {
        val ancestor = NodeId(targetNode.longValue + 1)
        val Node = mock<Node> { on { parentId }.thenReturn(ancestor) }
        val ancestorNode = mock<Node> { on { id }.thenReturn(ancestor) }

        fileRepository.stub {
            onBlocking { getNodeById(targetNode) }.thenReturn(Node)
            onBlocking { getNodeById(ancestor) }.thenReturn(ancestorNode)
        }
        val actual = underTest(targetNode, ancestor)
        assertThat(actual).isTrue()
    }

    @Test
    fun `test that false is returned if the target node is null`() = runTest {
        fileRepository.stub {
            onBlocking { getNodeById(targetNode) }.thenReturn(null)
        }
        val actual = underTest(targetNode, targetNode)
        assertThat(actual).isFalse()
    }

    @Test
    fun `test that true is returned if the parent of the direct parent is the ancestor`() =
        runTest {
            val ancestor = NodeId(targetNode.longValue + 1)
            val directParentId = NodeId(ancestor.longValue + 1)
            val Node = mock<Node> { on { parentId }.thenReturn(directParentId) }
            val directParent = mock<Node> { on { parentId }.thenReturn(ancestor) }
            val ancestorNode = mock<Node> { on { id }.thenReturn(ancestor) }

            fileRepository.stub {
                onBlocking { getNodeById(targetNode) }.thenReturn(Node)
                onBlocking { getNodeById(directParentId) }.thenReturn(directParent)
                onBlocking { getNodeById(ancestor) }.thenReturn(ancestorNode)
            }
            val actual = underTest(targetNode, ancestor)
            assertThat(actual).isTrue()
        }

    @Test
    fun `test that true is returned if distant ancestor is the ancestor`() = runTest {
        val ancestor = NodeId(targetNode.longValue + 1)
        val directParentId = NodeId(ancestor.longValue + 1)
        val Node = mock<Node> { on { parentId }.thenReturn(directParentId) }
        val directParent = mock<Node> {
            on { parentId }.thenReturn(directParentId,
                directParentId,
                directParentId,
                directParentId,
                ancestor)
        }
        val ancestorNode = mock<Node> { on { id }.thenReturn(ancestor) }

        fileRepository.stub {
            onBlocking { getNodeById(targetNode) }.thenReturn(Node)
            onBlocking { getNodeById(directParentId) }.thenReturn(directParent)
            onBlocking { getNodeById(ancestor) }.thenReturn(ancestorNode)
        }
        val actual = underTest(targetNode, ancestor)
        assertThat(actual).isTrue()
    }
}