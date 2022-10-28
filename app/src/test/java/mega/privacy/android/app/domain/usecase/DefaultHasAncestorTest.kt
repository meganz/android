package mega.privacy.android.app.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.repository.FilesRepository
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.HasAncestor
import nz.mega.sdk.MegaNode
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultHasAncestorTest {
    private lateinit var underTest: HasAncestor

    private val filesRepository = mock<FilesRepository>()
    private val targetNode = NodeId(12L)

    @Before
    fun setUp() {
        underTest = DefaultHasAncestor(filesRepository = filesRepository)
    }

    @Test
    fun `test that true is returned if the ids are the same`() = runTest {
        val megaNode = mock<MegaNode> { on { handle }.thenReturn(targetNode.id) }

        filesRepository.stub {
            onBlocking { getNodeByHandle(targetNode.id) }.thenReturn(megaNode)
        }

        val actual = underTest(targetNode, targetNode)
        assertThat(actual).isTrue()
    }

    @Test
    fun `test that true is returned if direct parent is the ancestor`() = runTest {
        val ancestor = NodeId(targetNode.id + 1)
        val megaNode = mock<MegaNode> { on { parentHandle }.thenReturn(ancestor.id) }
        val ancestorNode = mock<MegaNode> { on { handle }.thenReturn(ancestor.id) }

        filesRepository.stub {
            onBlocking { getNodeByHandle(targetNode.id) }.thenReturn(megaNode)
            onBlocking { getNodeByHandle(ancestor.id) }.thenReturn(ancestorNode)
        }
        val actual = underTest(targetNode, ancestor)
        assertThat(actual).isTrue()
    }

    @Test
    fun `test that false is returned if the target node is null`() = runTest {
        filesRepository.stub {
            onBlocking { getNodeByHandle(targetNode.id) }.thenReturn(null)
        }
        val actual = underTest(targetNode, targetNode)
        assertThat(actual).isFalse()
    }

    @Test
    fun `test that true is returned if the parent of the direct parent is the ancestor`() =
        runTest {
            val ancestor = NodeId(targetNode.id + 1)
            val directParentId = ancestor.id + 1
            val megaNode = mock<MegaNode> { on { parentHandle }.thenReturn(directParentId) }
            val directParent = mock<MegaNode> { on { parentHandle }.thenReturn(ancestor.id) }
            val ancestorNode = mock<MegaNode> { on { handle }.thenReturn(ancestor.id) }

            filesRepository.stub {
                onBlocking { getNodeByHandle(targetNode.id) }.thenReturn(megaNode)
                onBlocking { getNodeByHandle(directParentId) }.thenReturn(directParent)
                onBlocking { getNodeByHandle(ancestor.id) }.thenReturn(ancestorNode)
            }
            val actual = underTest(targetNode, ancestor)
            assertThat(actual).isTrue()
        }

    @Test
    fun `test that true is returned if distant ancestor is the ancestor`() = runTest {
        val ancestor = NodeId(targetNode.id + 1)
        val directParentId = ancestor.id + 1
        val megaNode = mock<MegaNode> { on { parentHandle }.thenReturn(directParentId) }
        val directParent = mock<MegaNode> {
            on { parentHandle }.thenReturn(
                directParentId,
                directParentId,
                directParentId,
                directParentId,
                ancestor.id,
            )
        }
        val ancestorNode = mock<MegaNode> { on { handle }.thenReturn(ancestor.id) }

        filesRepository.stub {
            onBlocking { getNodeByHandle(targetNode.id) }.thenReturn(megaNode)
            onBlocking { getNodeByHandle(directParentId) }.thenReturn(directParent)
            onBlocking { getNodeByHandle(ancestor.id) }.thenReturn(ancestorNode)
        }
        val actual = underTest(targetNode, ancestor)
        assertThat(actual).isTrue()
    }
}
