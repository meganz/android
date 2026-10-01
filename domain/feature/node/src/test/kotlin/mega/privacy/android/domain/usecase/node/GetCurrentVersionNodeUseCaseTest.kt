package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetCurrentVersionNodeUseCaseTest {

    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val getChildNodeUseCase = mock<GetChildNodeUseCase>()
    private lateinit var underTest: GetCurrentVersionNodeUseCase

    @BeforeEach
    fun setUp() {
        reset(getNodeByIdUseCase, getChildNodeUseCase)
        underTest = GetCurrentVersionNodeUseCase(getNodeByIdUseCase, getChildNodeUseCase)
    }

    @Test
    fun `test that null is returned when the node does not resolve`() = runTest {
        val nodeId = NodeId(1L)
        whenever(getNodeByIdUseCase(nodeId)).thenReturn(null)

        assertThat(underTest(nodeId)).isNull()
    }

    @Test
    fun `test that null is returned when the node is not a file`() = runTest {
        val nodeId = NodeId(1L)
        whenever(getNodeByIdUseCase(nodeId)).thenReturn(mock<TypedFolderNode>())

        assertThat(underTest(nodeId)).isEqualTo(null)
    }

    @Test
    fun `test that the node itself is returned when it is still the current child`() = runTest {
        val nodeId = NodeId(1L)
        val parentId = NodeId(10L)
        val node = mock<TypedFileNode> {
            on { id } doReturn nodeId
            on { this.parentId } doReturn parentId
            on { name } doReturn "notes.txt"
        }
        val sameChild = mock<TypedFileNode> { on { id } doReturn nodeId }
        whenever(getNodeByIdUseCase(nodeId)).thenReturn(node)
        // Parent is a folder, so the version-chain walk stops immediately.
        whenever(getNodeByIdUseCase(parentId)).thenReturn(mock<TypedFolderNode>())
        whenever(getChildNodeUseCase(parentId, "notes.txt")).thenReturn(sameChild)

        assertThat(underTest(nodeId)).isEqualTo(node)
    }

    @Test
    fun `test that the node itself is returned when no current child is found`() = runTest {
        val nodeId = NodeId(1L)
        val parentId = NodeId(10L)
        val node = mock<TypedFileNode> {
            on { id } doReturn nodeId
            on { this.parentId } doReturn parentId
            on { name } doReturn "notes.txt"
        }
        whenever(getNodeByIdUseCase(nodeId)).thenReturn(node)
        whenever(getNodeByIdUseCase(parentId)).thenReturn(mock<TypedFolderNode>())
        whenever(getChildNodeUseCase(parentId, "notes.txt")).thenReturn(null)

        assertThat(underTest(nodeId)).isEqualTo(node)
    }

    @Test
    fun `test that the current version is returned when a sibling version under the folder`() =
        runTest {
            // Version model A: the stale version's parent is the containing folder.
            val staleId = NodeId(1L)
            val currentId = NodeId(2L)
            val folderId = NodeId(10L)
            val staleNode = mock<TypedFileNode> {
                on { id } doReturn staleId
                on { this.parentId } doReturn folderId
                on { name } doReturn "notes.txt"
            }
            val currentNode = mock<TypedFileNode> { on { id } doReturn currentId }
            val currentChild = mock<TypedFileNode> { on { id } doReturn currentId }
            whenever(getNodeByIdUseCase(staleId)).thenReturn(staleNode)
            whenever(getNodeByIdUseCase(folderId)).thenReturn(mock<TypedFolderNode>())
            whenever(getChildNodeUseCase(folderId, "notes.txt")).thenReturn(currentChild)
            whenever(getNodeByIdUseCase(currentId)).thenReturn(currentNode)

            assertThat(underTest(staleId)).isEqualTo(currentNode)
        }

    @Test
    fun `test that the current version is returned when walking up the version chain`() = runTest {
        // Version model B: the stale version's parent is the newer (current) version, whose
        // parent is the folder. The walk climbs stale -> current, then confirms via the folder.
        val staleId = NodeId(1L)
        val currentId = NodeId(2L)
        val folderId = NodeId(10L)
        val staleNode = mock<TypedFileNode> {
            on { id } doReturn staleId
            on { this.parentId } doReturn currentId
            on { name } doReturn "notes.txt"
        }
        val currentNode = mock<TypedFileNode> {
            on { id } doReturn currentId
            on { this.parentId } doReturn folderId
            on { name } doReturn "notes.txt"
        }
        val currentChild = mock<TypedFileNode> { on { id } doReturn currentId }
        whenever(getNodeByIdUseCase(staleId)).thenReturn(staleNode)
        whenever(getNodeByIdUseCase(currentId)).thenReturn(currentNode)
        whenever(getNodeByIdUseCase(folderId)).thenReturn(mock<TypedFolderNode>())
        whenever(getChildNodeUseCase(folderId, "notes.txt")).thenReturn(currentChild)

        assertThat(underTest(staleId)).isEqualTo(currentNode)
    }
}
