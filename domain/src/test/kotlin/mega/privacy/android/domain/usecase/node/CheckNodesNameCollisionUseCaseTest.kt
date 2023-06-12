package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeNameCollision
import mega.privacy.android.domain.entity.node.NodeNameCollisionResult
import mega.privacy.android.domain.exception.node.NodeDoesNotExistsException
import mega.privacy.android.domain.repository.NodeRepository
import mega.privacy.android.domain.usecase.GetRootNodeUseCase
import mega.privacy.android.domain.usecase.IsNodeInRubbish
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import kotlin.random.Random

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CheckNodesNameCollisionUseCaseTest {
    private lateinit var underTest: CheckNodesNameCollisionUseCase

    private val isNodeInRubbish: IsNodeInRubbish = mock()
    private val getChildNodeUseCase: GetChildNodeUseCase = mock()
    private val getNodeByHandleUseCase: GetNodeByHandleUseCase = mock()
    private val getRootNodeUseCase: GetRootNodeUseCase = mock()
    private val repository: NodeRepository = mock()


    @BeforeAll
    fun setUp() {
        underTest = CheckNodesNameCollisionUseCase(
            isNodeInRubbish = isNodeInRubbish,
            getChildNodeUseCase = getChildNodeUseCase,
            getNodeByHandleUseCase = getNodeByHandleUseCase,
            getRootNodeUseCase = getRootNodeUseCase,
            nodeRepository = repository
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            isNodeInRubbish,
            getChildNodeUseCase,
            getChildNodeUseCase,
            getRootNodeUseCase,
            repository
        )
    }

    @Test
    fun `test that all nodes no conflict when all parent nodes are invalid and null`() = runTest {
        val nodes =
            mapOf(1L to INVALID_NODE_HANDLE, 2L to INVALID_NODE_HANDLE, 3L to INVALID_NODE_HANDLE)
        whenever(repository.getInvalidHandle()).thenReturn(INVALID_NODE_HANDLE)
        whenever(getRootNodeUseCase()).thenReturn(null)
        Truth.assertThat(underTest(nodes, NodeNameCollisionType.RESTORE)).isEqualTo(
            NodeNameCollisionResult(nodes, emptyMap(), NodeNameCollisionType.RESTORE)
        )
    }

    @Test
    fun `test that all nodes no conflict when can not find parent node`() = runTest {
        val nodes =
            mapOf(1L to 100L, 2L to 101L, 3L to 102L)
        whenever(repository.getInvalidHandle()).thenReturn(INVALID_NODE_HANDLE)
        whenever(getNodeByHandleUseCase(any())).thenReturn(null)
        Truth.assertThat(underTest(nodes, NodeNameCollisionType.RESTORE)).isEqualTo(
            NodeNameCollisionResult(nodes, emptyMap(), NodeNameCollisionType.RESTORE)
        )
    }

    @Test
    fun `test that all nodes no conflict when all parent nodes are in rubbish bin`() = runTest {
        val nodes =
            mapOf(1L to 100L, 2L to 101L, 3L to 102L)
        whenever(repository.getInvalidHandle()).thenReturn(INVALID_NODE_HANDLE)
        whenever(getNodeByHandleUseCase(any())).thenReturn(mock<FileNode>())
        whenever(isNodeInRubbish(any())).thenReturn(true)
        Truth.assertThat(underTest(nodes, NodeNameCollisionType.RESTORE)).isEqualTo(
            NodeNameCollisionResult(nodes, emptyMap(), NodeNameCollisionType.RESTORE)
        )
    }

    @Test
    fun `test that result returns correctly when one of node has name collision`() = runTest {
        val nodes =
            mapOf(1L to 100L, 2L to 101L, 3L to 102L)
        val currentNode = mock<FileNode> {
            on { id }.thenReturn(NodeId(2L))
            on { name }.thenReturn("current")
            on { size }.thenReturn(Random(1000L).nextLong())
            on { modificationTime }.thenReturn(System.currentTimeMillis())
            on { creationTime }.thenReturn(System.currentTimeMillis())
        }
        val parentConflictNode = mock<FolderNode> {
            on { id }.thenReturn(NodeId(101L))
            on { childFileCount }.thenReturn(Random(1000).nextInt())
            on { childFolderCount }.thenReturn(Random(1000).nextInt())
        }
        val conflictNode = mock<FileNode> {
            on { id }.thenReturn(NodeId(Random(1000L).nextLong()))
        }
        whenever(repository.getInvalidHandle()).thenReturn(INVALID_NODE_HANDLE)
        whenever(getNodeByHandleUseCase(100L)).thenReturn(mock<FolderNode>())
        whenever(getNodeByHandleUseCase(101L)).thenReturn(parentConflictNode)
        whenever(getNodeByHandleUseCase(102L)).thenReturn(mock<FolderNode>())
        whenever(isNodeInRubbish(any())).thenReturn(false)
        whenever(getNodeByHandleUseCase(1L)).thenReturn(mock<FolderNode>())
        whenever(getNodeByHandleUseCase(2L)).thenReturn(currentNode)
        whenever(getNodeByHandleUseCase(3L)).thenReturn(mock<FolderNode>())
        whenever(getChildNodeUseCase(NodeId(101L), currentNode.name)).thenReturn(conflictNode)

        Truth.assertThat(underTest(nodes, NodeNameCollisionType.RESTORE)).isEqualTo(
            NodeNameCollisionResult(
                mapOf(1L to 100L, 3L to 102L),
                mapOf(
                    2L to NodeNameCollision(
                        collisionHandle = conflictNode.id.longValue,
                        nodeHandle = 2L,
                        parentHandle = 101L,
                        name = currentNode.name,
                        size = currentNode.size,
                        childFolderCount = parentConflictNode.childFolderCount,
                        childFileCount = parentConflictNode.childFileCount,
                        lastModified = currentNode.modificationTime,
                        isFile = true
                    )
                ),
                NodeNameCollisionType.RESTORE,
            )
        )
    }

    @Test
    fun `test that throw NodeDoesNotExistsException when can not find node by handle`() = runTest {
        val nodes =
            mapOf(1L to 100L, 2L to 101L, 3L to 102L)
        whenever(repository.getInvalidHandle()).thenReturn(INVALID_NODE_HANDLE)
        whenever(getNodeByHandleUseCase(100L)).thenReturn(mock<FolderNode>())
        whenever(getNodeByHandleUseCase(101L)).thenReturn(mock<FolderNode>())
        whenever(getNodeByHandleUseCase(102L)).thenReturn(mock<FolderNode>())
        whenever(isNodeInRubbish(any())).thenReturn(false)
        whenever(getNodeByHandleUseCase(1L)).thenReturn(null)

        try {
            underTest(nodes, NodeNameCollisionType.RESTORE)
        } catch (e: Exception) {
            Truth.assertThat(e).isInstanceOf(NodeDoesNotExistsException::class.java)
        }
    }

    companion object {
        private const val INVALID_NODE_HANDLE = -1L
    }
}